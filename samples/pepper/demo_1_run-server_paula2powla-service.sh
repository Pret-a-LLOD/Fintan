#!/bin/bash
# Needs to run in background. Best use an independent terminal for logging.

cd powla/experimental/salt/swagger/python-server

# starting up the container
docker run -p 8080:8080 swagger_server



