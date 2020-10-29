# Import mappings
To use mappings with uploaded datasets import `type-mapping.ttl` into the following named graph `<http://schema.swissartresearch.net/rds/type-mapping>`.

# Wikidata mappings
Mappings for wikidata are hardcoded in the `{root}/rds-global/config/repositories/wikidata-lookup.ttl` and `{root}/rds-global-dev/config/repositories/wikidata-lookup.ttl`. You can update them by changing `lookup:typeBlockTemplate` parameter. If type-mapping.ttl is uploaded, you can use `generate-type-mappings-for-wikidata.sparql` script to generate value clause basing on the information from the ttl file.

# Update type mappings
To change mappings update `type-mapping.ttl` and then reimport them as it described in the "Import mappings" paragraph.

# Dataset labels
To assign dataset labels to datasets execute INSERT query from `dataset-labels.sparql`.

