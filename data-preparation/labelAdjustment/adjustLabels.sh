#!/bin/bash


predicates=$(<$2) #predicates that are used in order to attach labels to entities

#set up folders and blazegraph runner
mkdir requests
mkdir responses
mkdir output
cd utils/blazegraph-runner
sbt stage
cd ../..
#get number of triples for labels
#echo "SELECT (COUNT(*) AS ?count) WHERE { ?entity $predicates  ?ext . }" > requests/label_count.rq
echo "SELECT ?graph_name
       ( COUNT ( * ) AS ?count )
WHERE
  {
    GRAPH ?graph_name
      {
	?subject $predicates ?label .
      }
  }
GROUP BY ?graph_name" > requests/label_count_by_graph.rq
utils/blazegraph-runner/target/universal/stage/bin/blazegraph-runner select --journal=$1 --outformat=json requests/label_count_by_graph.rq  "responses/label_count_by_graph.json"

#USE THIS TO MATERIALIZE TTL FILES
#mkdir output
#python get_label_ttl_files.py --predicate_file $2 --blazegraph_journal $1
utils/blazegraph-runner/target/universal/stage/bin/blazegraph-runner load --journal=$1 --graph="http://schema.swissartresearch.net/rds/labels" --informat=turtle output/*.ttl


#USE THIS TO INSERT LABELS DIRECTLY IN JOURNAL FILE (NO TTL FILES WILL BE GENERATED)
python update_labels.py --predicate_file $2 --blazegraph_journal $1


#SCRAP
#label_count=$(jq -r '.results.bindings[0].count.value' label_count.json)
#echo "SELECT * WHERE { ?entity $predicates  ?ext . }" > ../../labels.rq
#./target/universal/stage/bin/blazegraph-runner select --journal=$1 --outformat=tsv same_as.rq  "../../labels.tsv"
#awk 'BEGIN{ FS = OFS = "\t" } { $1 = $1 FS "<http://schema.swissartresearch.net/ontology/rds#label>" }1' ../../labels.tsv > ../../labels.tsv 
