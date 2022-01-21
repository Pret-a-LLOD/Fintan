#!/bin/bash

# store the path of the fintan-core directory
base_dir=$(dirname -- "$(realpath -- "$0")")

mvn exec:java --quiet -e -Dfile.encoding=UTF8 -Dexec.mainClass=org.acoli.fintan.FintanCLIManager -Dexec.args="$*" --file="${base_dir}"
