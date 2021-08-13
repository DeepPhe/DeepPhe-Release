#Set this to the directory where neo4j is installed.
NEO4J_HOME="/opt/neo4j-community-3.5.12";

(rm -rf $NEO4J_HOME/data/databases/ontology.db && cd ../dphe-onto-db && cp -R src/main/resources/graph/neo4j/ontology.db $NEO4J_HOME/data/databases)

#copy the dphe-neo4j-plugin into the ne4j/plugins directory
cp ../dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.4.0.jar $NEO4J_HOME/plugins

$NEO4J_HOME/bin/neo4j console
