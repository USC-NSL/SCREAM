#!/bin/bash
rootFolder=/users/moshrefj/trace
traceFolder=/trace
i=$1
python -u $rootFolder/../scapy/sendmanypkt4.py --input $rootFolder/firstpkt.txt --output $traceFolder/$i.cap_- --mac=00:00:00:00:00:00:00:0${i} --scale 1 --timeBias -1 
mv $traceFolder/$i.cap_-_1 $traceFolder/$i.cap_0
