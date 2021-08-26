# Clean up any older build
rm -rf ../dphe-core
rm -rf ../dphe-neo4j
rm -rf ../dphe-neo4j-plugin
rm -rf ../dphe-stream
rm -rf ../dphe-onto-db
rm -rf ../dphe-examples
rm -rf ../DeepPhe-Viz-v2
rm -rf ../dphe-cli

#todo, setup ssh first so user doesn't have to enter username/pass
#Clone all necessary repos and checkout correct branches
git clone https://github.com/DeepPhe/dphe-core.git ../dphe-core
git clone https://github.com/DeepPhe/dphe-neo4j.git ../dphe-neo4j
git clone https://github.com/DeepPhe/dphe-neo4j-plugin.git ../dphe-neo4j-plugin
git clone https://github.com/DeepPhe/dphe-stream.git ../dphe-stream
git clone https://github.com/DeepPhe/dphe-onto-db.git ../dphe-onto-db
git clone https://github.com/DeepPhe/dphe-examples.git ../dphe-examples
git clone https://github.com/DeepPhe/DeepPhe-Viz-v2 ../DeepPhe-Viz-v2
git clone https://github.com/DeepPhe/dphe-cli.git ../dphe-cli

