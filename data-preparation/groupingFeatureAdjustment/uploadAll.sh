#!/bin/bash

export BLAZEGRAPH_ENDPOINT="https://rds-qa.swissartresearch.net/sparql"
export NAMED_GRAPH="http%3A%2F%2Frds.named-graph.com%2Fexact-match-statements"


for filename in ./*.ttl; do
    echo "\nUploading: ".${filename}
    curl -D- -L -u guest:guest -H "Content-Type: text/turtle" --upload-file ${filename} -X POST "${BLAZEGRAPH_ENDPOINT}?context-uri=${NAMED_GRAPH}"
done