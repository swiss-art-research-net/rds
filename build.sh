#!/bin/sh

VERSION=1.3.2

docker build . -f Dockerfile.rds-local -t "swissartresearx/rds-local:$VERSION" 
docker build . -f Dockerfile.rds-global -t "swissartresearx/rds-global:$VERSION" 
