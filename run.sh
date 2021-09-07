#!/bin/bash

# store the path of the root directory in HOME
HOME=$( dirname -- "$(realpath -- "$0")");


mvn exec:java -Dfile.encoding=UTF8 -Dexec.mainClass=de.unifrankfurt.informatik.acoli.fintan.FintanCLIManager -Dexec.args="$*"

