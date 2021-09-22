#!/bin/bash

# store the path of the root directory in HOME
HOME=$( dirname -- "$(realpath -- "$0")");

cd $HOME

mvn exec:java --quiet -e -Dfile.encoding=UTF8 -Dexec.mainClass=de.unifrankfurt.informatik.acoli.fintan.FintanCLIManager -Dexec.args="$*"

