#!/bin/bash
sudo mkdir -p /trace
sudo chown moshrefj /trace
h1=`hostname | cut -f1 -d. |cut -c5`
let h2=1+$h1
scp node$h1.computation.vcrib:/trace/trace2/$h2.cap /trace
