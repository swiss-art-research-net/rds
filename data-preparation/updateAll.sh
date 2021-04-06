ROOT_DIR=$(pwd)
UPDATE_SCRIPTS_DIR=${ROOT_DIR}/update-scripts

cd ${UPDATE_SCRIPTS_DIR}
./updateAat.sh
./updateGeonames.sh
./updateGndBibiliographic.sh
./updateGndFacts.sh
./updateUlan.sh
cd ${ROOT_DIR}
./uploadMetadata.sh