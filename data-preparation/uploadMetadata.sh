DATA_FORMAT="application/x-turtle"
# ================================================================
if [ -z "${BLAZEGRAPH_ENDPOINT}" ]; then
    BLAZEGRAPH_ENDPOINT="http://localhost:8080/blazegraph/namespace/kb/sparql"
fi
SCRIPT_DIR=$(pwd)
set -e
# ================================================================

echo "Start script uploadMetadata.sh."
echo "Upload labels to the blazegraph"
# ========================
curl -D- -L -u guest:guest -H "Content-Type: ${DATA_FORMAT}" --upload-file "./_datasetsMetadata.ttl" -X POST "${BLAZEGRAPH_ENDPOINT}"

echo "Script uploadMetadata.sh finished."