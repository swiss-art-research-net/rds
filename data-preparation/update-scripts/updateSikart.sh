#!/bin/bash
#   GITHUB_USERNAME (String)
#   GITHUB_TOKEN (String)


export DATA_DIRECTORY=$(pwd)/sikart-data
export DATA_URL="https://raw.githubusercontent.com/swiss-art-research-net/sikart-data/main/source/SIK_20210616_2300.ttl.zip"
# For the test in the platform put content of 'assets_to_tests' to
# 'runtime/assets' folder, then use follwoing line instead
# export DATA_URL=http://localhost:10214/assets/no_auth/geonames-data/geonames-data.zip
export FILE_FORMAT=ttl
export SKIP_DELETING=false
export USE_GUNZIP=false
export SKIP_UNZIPPING=false
export REPOSITORY_LOCATION="swiss-art-research-net"
export REPOSITORY_NAME="sikart-data"
DATA_FORMAT="application/x-turtle"
NAMED_GRAPH=http%3A%2F%2Frecherche.sik-isea%2Fgraph
# ================================================================
if [ -z "${BLAZEGRAPH_ENDPOINT}" ]; then
    BLAZEGRAPH_ENDPOINT="http://localhost:8081/blazegraph/namespace/kb/sparql"
fi
SCRIPT_DIR=$(pwd)
set -e
function urldecode() { : "${*//+/ }"; echo -e "${_//%/\\x}"; }
NAMED_GRAPH_DECODED="$(urldecode ${NAMED_GRAPH})"
# ================================================================

echo "Start script updateSikart.sh."
# ========================
./_downloadSecureLfsAndUnzip.sh

echo "Remove old data from the database"
curl --location --request POST "${BLAZEGRAPH_ENDPOINT}" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "update=DROP GRAPH <${NAMED_GRAPH_DECODED}>"

echo "Upload data to the database"
# ========================
for filename in ${DATA_DIRECTORY}/*.${FILE_FORMAT}; do
    echo "\nUploading: ".${filename}
    curl -D- -L -u guest:guest -H "Content-Type: ${DATA_FORMAT}" --upload-file ${filename} -X POST "${BLAZEGRAPH_ENDPOINT}?context-uri=${NAMED_GRAPH}"
done

echo "Script updateSikart.sh finished."
