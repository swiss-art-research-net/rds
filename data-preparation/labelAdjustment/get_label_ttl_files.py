import os
import argparse
import json

def main(predicate_file, blazegraph_journal):
    with open('responses/label_count_by_graph.json', 'r') as f:
        count_json = json.load(f)
    with open(predicate_file, 'r') as f:
        predicates = f.read()

    file_num = 0
    # total = int(count_json['results']['bindings'][0]['count']['value'])
    graph2nb = {count_json['results']['bindings'][i]['graph_name']['value'] : int(count_json['results']['bindings'][i]['count']['value']) for i in range(len(count_json['results']['bindings']))}
    #graph2nb = {'file:///_datasetsMetadata.ttl': 24}
    for graph, nb in graph2nb.items():
        counter = 0
        while counter <= nb:
            query = """
            CONSTRUCT {{ ?subject <http://schema.swissartresearch.net/ontology/rds#label> ?label }} WHERE {{
               GRAPH <{0}> {{
                    ?subject <http://www.researchspace.org/ontology/displayLabel>|<http://www.w3.org/2000/01/rdf-schema#label>|<http://www.w3.org/2004/02/skos/core#prefLabel>|<https://d-nb.info/standards/elementset/gnd#preferredName>|<http://www.w3.org/2008/05/skos-xl#literalForm>|<http://vocab.getty.edu/ontology#term>|<http://www.geonames.org/ontology#name>|<https://d-nb.info/standards/elementset/gnd#definition>|<https://d-nb.info/standards/elementset/gnd#preferredNameForTheWork>|<http://purl.org/ontology/bibo/shortTitle>|<http://xmlns.com/foaf/0.1/name> ?label .
              }}
            }} ORDER BY DESC(?subject) OFFSET {1} LIMIT {2}
            """.format(graph, str(counter), str(counter + 5000000))
            counter = counter + 5000000
            print(graph)
            with open('label_query.rq', 'w') as f:
                f.write(query)
            bash_command = 'utils/blazegraph-runner/target/universal/stage/bin/blazegraph-runner construct --journal={0} --outformat=turtle label_query.rq output/labels_{1}.ttl'.format(blazegraph_journal, file_num)
            os.system(bash_command)

            file_num = file_num + 1
            
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    
    parser = argparse.ArgumentParser(description = 'Produce ttl files with entities and their labels using a unified RDS predicate <http://schema.swissartresearch.net/ontology/rds#label>')
    parser.add_argument('--predicate_file', required=True,help='file with predicate to use to query for entity labels')
    parser.add_argument('--blazegraph_journal',required=True, help='path to blazegraph journal file')
    
    args = parser.parse_args()
    predicate_file = args.predicate_file
    blazegraph_journal = args.blazegraph_journal
    
    main(predicate_file, blazegraph_journal)
