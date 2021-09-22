#!/bin/bash

#in case mvn does not find java commands like java, javac, javadoc:
#uncomment the line below and adjust the path to your system
#JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64

# store the path of the conll-rdf directory in HOME
HOME=$( dirname -- "$(realpath -- "$0")");

cd $HOME
git clone https://github.com/acoli-repo/fintan-core.git
cd fintan-core
mvn --batch-mode --quiet -DskipTests clean install

cd $HOME
git clone -b fintan-support https://github.com/acoli-repo/conll-rdf.git
cd conll-rdf
mvn --batch-mode --quiet -DskipTests clean install

cd $HOME
git clone https://github.com/tarql/tarql.git
cd tarql
mvn --batch-mode --quiet -DskipTests -Dmaven.javadoc.skip=true clean install 

cd $HOME
mvn --batch-mode clean install
