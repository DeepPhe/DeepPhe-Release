#!/usr/bin/env bash

echo -e "\n"
echo "**************************************************************************"
echo "* This script copies a new/clean database into the Neo4j folder so a     *"
echo "* user can run DeepPhe on a new set of documents.                        *"
echo "*                                                                        *"
echo "* Copyright 2022, Boston Childrens Hospital, University of Pittsburgh    *"
echo "*                                                                        *"
echo "* Script Author: John Levander jdl50@pitt.edu                            *"
echo "**************************************************************************"

### Constants
filename="installer.conf"
neo4j_delay_in_sec=5
neo4j_home_key=NEO4J_HOME

source_location="../../"

handleError() {
  if [ $? -ne 0 ]
  then
    printf "\nError: [$rc]"; exit $rc
  else
    printf "\n  Success!\n\n"
  fi
}

. ${filename}

neo4j_path=$NEO4J_HOME

printf "\n"

printf "Updating neo4j.conf file...\n"

sed -i 's/#dbms\.active_database=graph.db/dbms\.active_database=ontology.db/' ${neo4j_path}/conf/neo4j.conf
handleError

printf "Copying a clean database from\n\t${source_location}dphe-onto-db/src/main/resources/graph/neo4j/ontology.db\n   to\n\t${neo4j_path}/data/databases"

(rm -rf ${neo4j_path}/data/databases/ontology.db && cd ${source_location}dphe-onto-db && cp -R src/main/resources/graph/neo4j/ontology.db ${neo4j_path}/data/databases)

handleError

printf "Copying dphe-neo4j-plugin from\n\t${source_location}dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.5.0.jar\n   to\n\t${neo4j_path}/plugins"
cp ${source_location}dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.5.0.jar  ${neo4j_path}/plugins

handleError
num_deepphe_plugins=$(ls -l ${neo4j_path}/plugins/deepphe*.jar | wc -l)
if [ $num_deepphe_plugins -gt 1 ]; then
    echo -e "\nERROR: It appears there are two DeepPhe plugins in the ${neo4j_path}/plugins directory.  Please remove all DeepPhe plugins from that directory and restart this script."
    exit
fi
