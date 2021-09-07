#!/bin/bash

HOME=`echo $0 | sed -e s/'^[^\/]*$'/'.'/g -e s/'\/[^\/]*$'//`;
ROOT=$HOME/../../..;


$ROOT/run.sh -c samples/xslt/apertium/_demo-apertium-full-with-tiad.json -p https://github.com/apertium/apertium-trunk.git en es


