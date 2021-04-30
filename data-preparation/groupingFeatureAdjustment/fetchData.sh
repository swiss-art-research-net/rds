#!/bin/bash

mkdir -p ./data

if [ -z "${BLAZEGRAPH_ENDPOINT}" ]; then
    BLAZEGRAPH_ENDPOINT="http://localhost:8080/blazegraph/namespace/kb/sparql"
fi

if [ -z "${WIKIDATA_ENDPOINT}" ]; then
    WIKIDATA_ENDPOINT="https://query.wikidata.org/sparql"
fi

function xfetchData() {
  local dataset="$1"
  echo >&2 "Skipped fetching of dataset $dataset: ignore"
}
function fetchData() {
  local dataset="$1"
  local endpoint="$2"
  local query="$3"
  local outputFile="$4"

  if [ -f "$outputFile" ]; then
    echo >&2 "Skipped fetching of dataset $dataset: file $outputFile already exists"
    return
  fi

  echo >&2 "Fetching dataset $dataset..."
  curl --location --request POST "$endpoint" \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --header 'Accept: text/turtle' \
    --data-urlencode "query=$query" > "$outputFile"
  local rc=$?
  if [ $rc -ne 0 ]; then
    echo >&2 "Failed to fetch dataset $dataset: curl returned $rc"
    return $rc
  fi
}


# AAT
fetchData "AAT" ${BLAZEGRAPH_ENDPOINT} 'PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  GRAPH <http://vocab.getty.edu/aat/graph> {
    ?candidate2 (skos:exactMatch | skos:closeMatch) ?candidate1 .
  }
}' ./data/aatSameAs.ttl

# ULAN
fetchData "ULAN" ${BLAZEGRAPH_ENDPOINT} 'PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  GRAPH <http://vocab.getty.edu/ulan/graph> {
    ?candidate2 (skos:exactMatch | skos:closeMatch) ?candidate1 .
  }
}' ./data/ulanSameAs.ttl

# Bibliographic data
fetchData "Bibliographic data" ${BLAZEGRAPH_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  GRAPH <https://d-nb.info/gnd/bibliographicdata/graph> {
    ?candidate2 owl:sameAs ?candidate1 .
  }
}' ./data/bibliographicDataSameAs.ttl

# Entityfacts
fetchData "Entityfacts-1" ${BLAZEGRAPH_ENDPOINT} 'PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX schema: <http://schema.org/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  {SELECT * WHERE {
      VALUES (?predicate) {
          (schema:sameAs)
          (rdfs:seeAlso)
          (skos:exactMatch)
      }
      GRAPH <https://d-nb.info/gnd/entityfacts/graph> {
      ?candidate2 ?predicate ?candidate1 .
      }
  } LIMIT 20000000}
}' ./data/entityfactsSameAs_1.ttl

fetchData "Entityfacts-2" ${BLAZEGRAPH_ENDPOINT} 'PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX schema: <http://schema.org/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  {SELECT * WHERE {
      VALUES (?predicate) {
          (schema:sameAs)
          (rdfs:seeAlso)
          (skos:exactMatch)
      }
      GRAPH <https://d-nb.info/gnd/entityfacts/graph> {
      ?candidate2 ?predicate ?candidate1 .
      }
  } OFFSET 20000000 LIMIT 20000000}
}' ./data/entityfactsSameAs_2.ttl

# LOC
fetchData "LOC" ${BLAZEGRAPH_ENDPOINT} 'PREFIX loc: <http://www.loc.gov/mads/rdf/v1#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  GRAPH <http://vocab.getty.edu/loc/graph> {
    ?candidate2 loc:hasCloseExternalAuthority ?candidate1 .
  }
}' ./data/locSameAs.ttl

# BNF
fetchData "BNF" ${BLAZEGRAPH_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  GRAPH <http://vocab.getty.edu/bnf/graph> {
    ?candidate2 (owl:sameAs | foaf:focus) ?candidate1 .
  }
}' ./data/bnfSameAs.ttl

# VIAF
fetchData "VIAF" ${BLAZEGRAPH_ENDPOINT} 'PREFIX schema: <http://schema.org/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
CONSTRUCT { ?candidate2 owl:sameAs ?candidate1 . } WHERE {
  GRAPH <http://vocab.getty.edu/viaf/graph> {
    ?candidate2 schema:sameAs ?candidate1 .
  }
}' ./data/viafSameAs.ttl


# Wikidata
# ==============================================================

# Wikidata VIAF-1
fetchData "Wikidata VIAF-1" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_1.ttl

# Wikidata VIAF-2
fetchData "Wikidata VIAF-2" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } OFFSET 500000 LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_2.ttl

# Wikidata VIAF-3
fetchData "Wikidata VIAF-3" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } OFFSET 1000000 LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_3.ttl

# Wikidata VIAF-4
fetchData "Wikidata VIAF-4" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } OFFSET 1500000 LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_4.ttl

# Wikidata VIAF-5
fetchData "Wikidata VIAF-5" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } OFFSET 2000000 LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_5.ttl

# Wikidata VIAF-6
fetchData "Wikidata VIAF-6" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } OFFSET 2500000 LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_6.ttl

# Wikidata VIAF-7
fetchData "Wikidata VIAF-7" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
    {SELECT ?candidate ?sameAsID WHERE {
        ?candidate wdt:P214 ?sameAsID .
    } OFFSET 3000000 LIMIT 500000}
    BIND (IRI(CONCAT("http://viaf.org/viaf/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataSameAsVIAF_7.ttl

# Wikidata GND
fetchData "Wikidata GND" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
  ?candidate wdt:P227 ?sameAsID .
  BIND (IRI(CONCAT("https://d-nb.info/gnd/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataGNDSameAs.ttl

# Wikidata BNF
fetchData "Wikidata BNF" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
  ?candidate wdt:P268 ?sameAsID .
  BIND (IRI(CONCAT("http://data.bnf.fr/ark:/12148/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataBNFSameAs.ttl

# Wikidata LOC
fetchData "Wikidata LOC" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
  ?candidate wdt:P244 ?sameAsID .
  BIND (IRI(CONCAT("http://id.loc.gov/authorities/names/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataLOCSameAs.ttl

# Wikidata ULAN
fetchData "Wikidata ULAN" ${WIKIDATA_ENDPOINT} 'PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
CONSTRUCT {
    ?candidate owl:sameAs ?sameAs .
} WHERE {
  ?candidate wdt:P245 ?sameAsID .
  BIND (IRI(CONCAT("http://vocab.getty.edu/ulan/", ?sameAsID)) as ?sameAs)
}' ./data/wikidataUlanSameAs.ttl
