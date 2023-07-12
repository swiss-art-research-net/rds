export DATA_DIRECTORY=./ulan-data
export DATA_URL=http://ulandownloads.getty.edu/VocabData/full.zip
# For the test in the platform put content of 'assets_to_tests' to
# 'runtime/assets' folder, then use follwoing line instead
# export DATA_URL=http://localhost:10214/assets/no_auth/ulan-data/data.zip
export FILE_FORMAT=nt
DATA_FORMAT="text/plain"
NAMED_GRAPH=http%3A%2F%2Fvocab.getty.edu%2Fulan%2Fgraph
# ================================================================
if [ -z "${BLAZEGRAPH_ENDPOINT}" ]; then
    BLAZEGRAPH_ENDPOINT="http://localhost:8080/blazegraph/namespace/kb/sparql"
fi
SCRIPT_DIR=$(pwd)
set -e
function urldecode() { : "${*//+/ }"; echo -e "${_//%/\\x}"; }
NAMED_GRAPH_DECODED="$(urldecode ${NAMED_GRAPH})"
# ================================================================

echo "Start script updateUlan.sh."
# ========================
./_downloadAndUnzip.sh

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

echo "Script updateUlan.sh finished."
