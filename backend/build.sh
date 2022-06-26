#!/bin/bash

#in case mvn does not find java commands like java, javac, javadoc:
#uncomment the line below and adjust the path to your system
#JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64

# store the path of the fintan-core directory
base_dir=$(dirname -- "$(realpath -- "$0")")

cd "${base_dir}" && git submodule update --init --recursive

mvn --batch-mode --quiet --file="${base_dir}/parent" \
    --also-make --projects :fintan-backend \
    -DskipTests -Dmaven.javadoc.skip \
    clean package
