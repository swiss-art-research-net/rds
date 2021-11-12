export DATA_DIRECTORY=$(pwd)/gnd-facts-data
export DATA_URL=https://data.dnb.de/opendata/authorities_entityfacts.jsonld.gz
# For the test in the platform put content of 'assets_to_tests' to
# 'runtime/assets' folder, then use follwoing line instead
# export DATA_URL=http://localhost:10214/assets/no_auth/gnd-facts-data/data.jsonld.gz
export FILE_FORMAT=jsonld
DATA_FORMAT="application/ld+json"
NAMED_GRAPH=https%3A%2F%2Fd-nb.info%2Fgnd%2Fentityfacts%2Fgraph
# ================================================================
if [ -z "${BLAZEGRAPH_ENDPOINT}" ]; then
    BLAZEGRAPH_ENDPOINT="http://localhost:8080/blazegraph/namespace/kb/sparql"
fi
SCRIPT_DIR=$(pwd)
set -e
if ! command -v jq &> /dev/null
then
    echo "JQ could not be found please install it before using this script"
    exit
fi
function urldecode() { : "${*//+/ }"; echo -e "${_//%/\\x}"; }
NAMED_GRAPH_DECODED="$(urldecode ${NAMED_GRAPH})"
# ================================================================

echo "Start script updateGndFacts.sh."
# ========================
USE_GUNZIP=true ./_downloadAndUnzip.sh

echo "Prepare GNDFacts data"
# ========================
cd ${DATA_DIRECTORY}
echo "Arrange data by lines using JQ. It can take quite some time... (Note: if this operation fails, your machine might require more memory)"
cat data.jsonld | jq -c .[] > data_temp.jsonld

echo "Split file by protions"
split --additional-suffix '.part' -l 7000 data_temp.jsonld
# gsplit --additional-suffix '.part' -l 7000 data_temp.jsonld # For MacOs
# rm ./data_temp.jsonld
echo "Format each file and remove temporary files."
echo
for filename in ./*.part; do
    echo -e "Format ${filename}.."
    cat ${filename} | jq -s . > ${filename}.jsonld
    rm ${filename}
done
cd ${SCRIPT_DIR}

echo "Remove old data from the database"
curl --location --request POST "${BLAZEGRAPH_ENDPOINT}" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "update=DROP GRAPH <${NAMED_GRAPH_DECODED}>"

echo "Upload data to the database"
# ========================
for filename in ${DATA_DIRECTORY}/*.part.${FILE_FORMAT}; do
    echo "\nUploading: ".${filename}
    curl -D- -L -u guest:guest -H "Content-Type: ${DATA_FORMAT}" --upload-file ${filename} -X POST "${BLAZEGRAPH_ENDPOINT}?context-uri=${NAMED_GRAPH}"
done

echo "Script updateGndFacts.sh finished."
