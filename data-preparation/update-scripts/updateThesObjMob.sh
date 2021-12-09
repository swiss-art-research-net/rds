export DATA_DIRECTORY=./thes-obj-mob-data
export DATA_URL="http://data.culture.fr/thesaurus/data/ark:/67717/T69?includeSchemes=true&format=TURTLE"
export FILE_FORMAT=ttl
DATA_FORMAT="text/turtle"
NAMED_GRAPH=http%3A%2F%2Fdata.culture.fr%2Fthesaurus%2Fresource%2Fark%3A%2F67717%2Fgraph
# ================================================================
if [ -z "${BLAZEGRAPH_ENDPOINT}" ]; then
    BLAZEGRAPH_ENDPOINT="http://localhost:8080/blazegraph/namespace/kb/sparql"
fi
SCRIPT_DIR=$(pwd)
set -e
function urldecode() { : "${*//+/ }"; echo -e "${_//%/\\x}"; }
NAMED_GRAPH_DECODED="$(urldecode ${NAMED_GRAPH})"
# ================================================================

echo "Start script updateThesObjMob.sh."
# ========================
./_downloadAndUnzip.sh

mv ${DATA_DIRECTORY}/data.zip ${DATA_DIRECTORY}/data.ttl

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

echo "Script updateThesObjMob.sh finished."