(cd ../dphe-cli/target && unzip -o deepphe-0.4.0-bin.zip && cd deepphe-0.4.0 && java -Xmx3g -Xms512M -classpath ./resources/:./lib/* org.apache.ctakes.core.pipeline.PiperFileRunner -p pipeline/DeepPhe.piper -i ../../../dphe-examples/reports -o ../../../output/DeepPhe -r bolt://localhost:7687 --user neo4j --pass neo4jpass)


