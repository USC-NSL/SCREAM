#!/bin/bash
#apt-get -y install python-scapy
if [ ! -r /trace/$1.txt ]
then
  echo "cp"
  mkdir -p /trace
  cp /users/moshrefj/trace/$1.txt /trace/	
fi
python -u /users/moshrefj/scapy/sendmanypkt4.py --input /trace/$1.txt --output /trace/$1.cap --mac=00:00:00:00:00:00:00:0${1} --scale 100 > /trace/$1.log
#cp /trace/$1.cap* /users/moshrefj/trace/
