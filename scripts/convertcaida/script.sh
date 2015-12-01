#!/bin/bash
gunzip -c $1.gz> /tmp/$1
echo "run tcpdump"
tcpdump -qns 0 -A -r /tmp/$1 >/tmp/$1.tmp
echo "run awk"
awk -f extract.awk /tmp/$1.tmp >/tmp/$1.csv
java -jar  ../convertcaidatrace/tracereader.jar -i /tmp/$1.csv -o $1.csv


