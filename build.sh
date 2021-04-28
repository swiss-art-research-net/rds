#!/bin/sh

VERSION=latest

docker build . -f Dockerfile.rds-local -t "swissartresearx/rds-local:$VERSION" 
