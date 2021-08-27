#!/bin/bash

git clone --depth 1 --branch v20210827 https://github.com/DeepPhe/dphe-examples.git

_dir=$('pwd')

mkdir -p output

(cd ../dphe-cli/target/deepphe-0.4.0-bin/deepphe-0.4.0 && java -Xmx3g -Xms512M -classpath ./resources/:./lib/* org.apache.ctakes.core.pipeline.PiperFileRunner -p pipeline/DeepPhe.piper -i $_dir/dphe-examples/reports/ -o $_dir/output -r bolt://localhost:7687 --user neo4j --pass neo4jpass)


