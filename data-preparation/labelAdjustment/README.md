# Label Adjustment

In order to execute the label adjustment pipeline -
1. Create a text file containing one line with the predicate that attaches entities to their labels (this should be in SPARQL format and hence it can include multiple predicates through the use of `|` and/or `/`.
1. Change the file `adjustLabels.sh` depending on whether `ttl` files should be materialized.
1. Execute `adjustLabels.sh`.
