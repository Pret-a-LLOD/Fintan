#!/bin/bash

HERE=`echo $0 | sed -e s/'^[^\/]*$'/'.'/g -e s/'\/[^\/]*$'//`;
ROOT=$HERE/../../..;


$ROOT/run.sh -c _demo-apertium-portable.json -p https://github.com/apertium/apertium-trunk.git en es


