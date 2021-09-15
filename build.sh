#!/bin/bash

# store the path of the conll-rdf directory in HOME
HOME=$( dirname -- "$(realpath -- "$0")");

cd $HOME
git clone https://github.com/acoli-repo/fintan-core.git
cd fintan-core
mvn --batch-mode --quiet -DskipTests clean install

cd $HOME
git clone https://github.com/acoli-repo/conll-rdf.git
cd conll-rdf
mvn --batch-mode --quiet -DskipTests clean install

cd $HOME
git clone https://github.com/tarql/tarql.git
cd tarql
mvn --batch-mode --quiet -DskipTests clean install

cd $HOME
mvn --batch-mode clean install
