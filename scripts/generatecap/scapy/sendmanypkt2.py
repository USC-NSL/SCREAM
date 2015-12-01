#! /usr/bin/env python

import sys
import math
import logging
#logging.getLogger("scapy.runtime").setLevel(logging.ERROR)
from scapy.all import send,Ether,IP,TCP,conf,ltoa,sendp
from time import time,sleep
from Queue import Queue
from threading import Thread
import socket, struct
import argparse

#sends packets to the IP to cover dataSize payload
def sendPerSecond(ip,dataSize,interface,mac):
  t=time();
  numPacket=int(dataSize/pktSize)
  plow=Ether(dst=mac)/IP(src=ip)/TCP();
  p=plow/completePayload;
  for i in range(numPacket):
    sendp(p,verbose=0,iface=interface)
  remainedData=dataSize-numPacket*pktSize
  if (remainedData>0):
    sendp(plow/''.zfill(remainedData),verbose=0,iface=interface)
#  print (time()-t)

# load data from file f until timestamp is >=end
# returns list of eligible packets and a packet that for the next round
def readFile(f,end):
  print("load until "+str(end))
  global nextRoundPacket;
  output=[]
  if (nextRoundPacket[0]>0):
    output.append((nextRoundPacket[1],nextRoundPacket[2]));
  while (True):
    line=f.readline();
    if (len(line)==0):
      break;
    data=line.rstrip('\n').split(',')
    t=int(data[0])/1000000;
    ip=int(data[1]);
    size=int(float(data[-1]));
    if (t>=end):
      nextRoundPacket=(t,ip,size)
      return output;
    output.append((ip,size));
  nextRoundPacket=(-1,0,0)
  return output;

##############################################3
def findInterface():
  for r in conf.route.routes:
    if (r[3]!='lo'):
      return r[3];
  return '';

###############################################
def fixRouting():
  for r in conf.route.routes:
    if (r[3]!='lo'):
      conf.route.delt(net=ltoa(r[0])+'/8',dev=r[3])
      conf.route.add(net='0.0.0.0/0',dev=r[3]);
      break;
  conf.route

###########################################
def convertIP(ip):
  return socket.inet_ntoa(struct.pack('!L',ip));

###########################3
def ip2long(ip):
    """
    Convert an IP string to long
    """
    packedIP = socket.inet_aton(ip)
    return struct.unpack("!L", packedIP)[0]

###########################
def getFirstTimestamp(filename):
  with open(filename,'r') as f:
    data=f.readline();
    return int(data.split(',')[0])/1000000

#unsigned right binary shift
def rshift(val, n): return (val % 0x100000000) >> n

#worker for multithreading
def worker():
    while True:
        (ip,size) = q.get()
        if (ip<0):
          q.task_done()
          return;
        sendPerSecond(convertIP(ip),size,interface,mac);
        q.task_done()
#################################################################
def matchFilters(ip,srcFiltersZip):
  for d,w in srcFiltersZip: 
    if (rshift(ip,w)==d):
      return True;
  return False;  

#################################################################
#parse params
parser = argparse.ArgumentParser(description='Generate traffic')
parser.add_argument('--input', required=True)
parser.add_argument('--srcFilter',default=['0.0.0.0/0'],nargs='*')
parser.add_argument('--start',default=-1,type=int)
parser.add_argument('--interface',default='')
parser.add_argument('--mac',required=True)

args=vars(parser.parse_args());
inputFile=args['input'];
startEvent=args['start'];
mac=args['mac'];
srcFilters=args['srcFilter'];
srcFiltersZip=[];
for f in srcFilters:
  f2=f.split('/');
  if (len(f2)>1): #has a wildcard setting
    w=32-int(f2[1]);
  else:
    w=0;
  srcFiltersZip.append((rshift(ip2long(f2[0]),w),w));

interface=args['interface'];
if (len(interface)==0):
  interface=findInterface();

#constants
pktSize=1460
completePayload=''.zfill(pktSize);
nextRoundPacket=(-1,0,0);
num_worker_threads=2

if (len(interface)==0):
  print("interface not found");
  exit(1);
#fixRouting(); //routing adds delay
#print(conf.route)

timestamp=getFirstTimestamp(inputFile);

#start workers
q = Queue()
for i in range(num_worker_threads):
     t = Thread(target=worker)
     t.start()
try:
  with open(inputFile,'r') as f:
    while(True):
      t1=time();
      timestamp+=1;
      toSend=readFile(f,timestamp)
      if (len(toSend)==0):
        break;
      i=0;
      for ip,size in toSend:
        if (matchFilters(ip,srcFiltersZip)):
          i+=1;
          q.put((ip,size))
#          sendPerSecond(convertIP(ip),size);
      print('added '+str(i)+" has now "+str(q.qsize()))
      q.join();
      toSleep=1-(time()-t1);
      if (toSleep>0):
        sleep(toSleep);
      else:
        print('stay behind '+str(-toSleep));
      
finally:
#instead of daemon let them finish clean
  for i in range(num_worker_threads):
    q.put((-1,0));

  q.join()       # block until all tasks are done

