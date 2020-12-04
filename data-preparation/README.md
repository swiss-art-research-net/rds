# How to use uploading scripts
To download and upload all datasets to your blazegraph execute `updateAll.sh`. This script automatically fetch, convert and uploads all necessary data. You can use scripts from the `update-scripts` folder to update only specific dataset (Scripts to use: updateAat.sh, updateGeonames.sh, updateGndBibiliographic.sh, updateGndFacts.sh, updateUlan.sh). All scripts which name start from underscore are internal and are not meant to be executed manually.
You can use `BLAZEGRAPH_ENDPOINT` variable to specify SPARQL endpoint to use. By default it's http://localhost:8080/blazegraph/namespace/kb/sparql.

### Requirements
Before using this script you should ensure you have 'sed' and 'docker' and 'jq' installed on you system. Typically 'docker' and 'sed' are installed. So use `apt-get` or `yum` to install 'jq' before using scripts.

### Example
```
export BLAZEGRAPH_ENDPOINT=http://rds-mph-blazegraph:8080/blazegraph/sparql

./uploadAll.sh
```
or
```
BLAZEGRAPH_ENDPOINT=http://rds-mph-blazegraph:8080/blazegraph/sparql ./uploadAll.sh
```

# Import mappings
To use mappings with uploaded datasets import `./manual-preparation/type-mapping.ttl` into the following named graph `<http://schema.swissartresearch.net/rds/type-mapping>`.

# Wikidata mappings
Mappings for wikidata are hardcoded in the `{root}/rds-global/config/repositories/wikidata-lookup.ttl` and `{root}/rds-global-dev/config/repositories/wikidata-lookup.ttl`. You can update them by changing `lookup:typeBlockTemplate` parameter. If type-mapping.ttl is uploaded, you can use `./manual-preparation/generate-type-mappings-for-wikidata.sparql` script to generate value clause basing on the information from the ttl file.

# Update type mappings
To change mappings update `./manual-preparation/type-mapping.ttl` and then reimport them as it described in the "Import mappings" paragraph.

# Dataset labels
To assign dataset labels to datasets via SPARQL execute INSERT query from `./manual-preparation/dataset-labels.sparql`.

