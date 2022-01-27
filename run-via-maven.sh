#!/bin/bash

# store the path of the fintan-core directory
base_dir=$(dirname -- "$(realpath -- "$0")")

# join(char seperator, arg1 ... argn) concatenates an array with the seperating character
function join {
  local IFS="$1"
  shift
  printf '%s\n' "$*"
}

# $@ is a special variable containing the arguments passed to the shell script.
# ${variable@Q} is "Parameter Transformation" to quote the content of the array for re-use
# as the variable is $@ is this case, ${}
# $@ is the variable of the arguments. For a wrapper to java you'd use ("$@")
args_array=("${@@Q}")
# join the quoted args into a single string
args=$(join ' ' "${args_array[@]}")

mvn exec:java --quiet -e -Dfile.encoding=UTF8 -Dexec.mainClass=org.acoli.fintan.FintanCLIManager -Dexec.args="$args" --file="${base_dir}"
