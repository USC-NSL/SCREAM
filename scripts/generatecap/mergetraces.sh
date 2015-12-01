#!/bin/bash
traceFolder=/trace/
trace2Folder=/trace/trace2
mkdir -p $trace2Folder
python mergetraces.py $traceFolder/$1.cap_ $trace2Folder/$1.cap
