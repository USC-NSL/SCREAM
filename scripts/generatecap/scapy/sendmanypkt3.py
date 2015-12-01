#! /usr/bin/env python

import sys
import math
import logging
#logging.getLogger("scapy.runtime").setLevel(logging.ERROR)
from scapy.all import send,Ether,IP,TCP,conf,ltoa,sendp,wrpcap,fragment,sendpfast
from time import time,sleep
from Queue import Queue
from threading import Thread
import socket, struct
import argparse
from operator import attrgetter

#sends packets to the IP to cover dataSize payload

def sendPerSecond(timestamp,ip,dataSize,mac,packets):  
#  p=Ether(dst=mac)/IP(src=ip)/TCP()/''.zfill(dataSize);
  t=time();
  p['IP'].src=ip;
  p2=fragment(p/''.zfill(dataSize))
  print(str(size)+','+str(time()-t))
  t=time();
  packets.extend(p2);
  print(str(size)+','+str(time()-t))
  t=time();
  #packets.extend(map(setTime,p,[timestamp]*len(p)));

def setTime(p,timestamp):
  p.time=timestamp
  return p;

def sendPerSeconda(timestamp,ip,dataSize,mac,packets):  
  numPacket=int(dataSize/pktSize)
  for i in range(numPacket):
    sendPerSecond(timestamp,ip,pktSize,mac,packets);
  remainedData=dataSize-numPacket*pktSize
  if (remainedData>0):
    sendPerSecond(timestamp,ip,remainedData,mac,packets); 

def sendPerSecondc(timestamp,ip,dataSize,mac,packets):  
  numPacket=int(dataSize/pktSize)
  if (numPacket>0):
    if (ip in packetsMap):
	p=packetsMap[ip];
    else:      
      p=Ether(dst=mac)/IP(src=ip)/TCP()/completePayload;
      packetsMap[ip]=p;
    p.time=timestamp;
    for i in range(numPacket):
      packets.append(p);
  
  remainedData=dataSize-numPacket*pktSize
  if (remainedData>0):
    if (ip in packetsMap2):
      p=packetsMap2[ip];
      p['Raw'].load=''.zfill(remainedData);
    else:      
      p=Ether(dst=mac)/IP(src=ip)/TCP()/''.zfill(remainedData)
      packetsMap2[ip]=p
    p.time=timestamp;
    packets.append(p);

def sendPerSecondb(timestamp,ip,dataSize,mac,packets):  
  numPacket=int(dataSize/pktSize)
  plow=Ether(dst=mac)/IP(src=ip)/TCP();
  p=plow/completePayload;
  for i in range(numPacket):
    p.time=timestamp+0.5*i/numPacket;
    packets.append(p);
  remainedData=dataSize-numPacket*pktSize
  if (remainedData>0):
    p=plow/''.zfill(remainedData)
    p.time=timestamp+0.5;
    packets.append(p);

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
    t=int(data[0]);
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
    return int(data.split(',')[0])

#unsigned right binary shift
def rshift(val, n): return (val % 0x100000000) >> n

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
parser.add_argument('--output', required=True)
parser.add_argument('--srcFilter',default=['0.0.0.0/0'],nargs='*')
parser.add_argument('--start',default=-1,type=int)
parser.add_argument('--mac',required=True)

args=vars(parser.parse_args());
inputFile=args['input'];
outputFile=args['output'];
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

#constants
pktSize=1460
timestampShift=1382990000;
completePayload=''.zfill(pktSize);
nextRoundPacket=(-1,0,0);
sizePerFile=1000000000

#fixRouting(); //routing adds delay
#print(conf.route)

timestamp=getFirstTimestamp(inputFile);
fileNum=1;

p=Ether(dst=mac)/IP()/TCP();
packetsMap={};
packetsMap2={};

#start workers
with open(inputFile,'r') as f:
  packets=[];
  tempPackets=[]
  totalSize=0;
  t=time();
  while(True):
    timestamp+=1;
    toSend=readFile(f,timestamp)
    if (len(toSend)==0):
      break;
    i=0;
    print('read '+str(time()-t));
    t=time();
    for ip,size in toSend:
      size=int(size/10);
      #if (matchFilters(ip,srcFiltersZip)):
      #totalSize+=size;
      #i+=1;
      sendPerSecondc(timestamp+timestampShift,convertIP(ip),size,mac,tempPackets);    
#    tempPackets.sort(key=attrgetter('time'))
    print('packetize '+str(time()-t));
    t=time();
    sendpfast(tempPackets,iface='eth0')
    print('send '+str(time()-t));
    t=time();
#    packets.extend(tempPackets);
    del tempPackets[:];
#    if (totalSize>sizePerFile):
#      wrpcap(outputFile+'_'+str(fileNum),packets);
#      totalSize=0;
#      fileNum+=1;
#      del packets[:];
#  if (len(packets)>0):	
#    wrpcap(outputFile+'_'+str(fileNum),packets);
