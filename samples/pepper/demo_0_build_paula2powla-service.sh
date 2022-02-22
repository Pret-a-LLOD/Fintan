#!/bin/bash
# Requires git, docker packages to be installed and properly configured.
# If docker does not run out of the box, try running it with sudo (only for testing!)

MYHOME=pwd

# cloning the server
git pull https://github.com/acoli-repo/powla.git
cd powla/experimental/salt/swagger/python-server

# building the image
docker build -t swagger_server .

cd $MYHOME

# download sample data
mkdir data
wget -o data/pcc2_PAULA.zip http://amir-zeldes.github.io/download/pcc2_PAULA.zip
