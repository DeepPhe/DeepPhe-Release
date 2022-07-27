#!/usr/bin/env bash

echo -e "\n"
echo "**************************************************************************"
echo "* This script runs the DeepPhe system on a set of documents.             *"
echo "*                                                                        *"
echo "* Usage:                                                                 *"
echo "*    ./run.sh path_to_document_dir                                       *"
echo "*                                                                        *"
echo "* Copyright 2022, Boston Childrens Hospital, University of Pittsburgh    *"
echo "*                                                                        *"
echo "* Script Author: John Levander jdl50@pitt.edu                            *"
echo "**************************************************************************"

filename="installer.conf"
neo4j_delay_in_sec=5
source_location="../../"
. ${filename}

neo4j_path=$NEO4J_HOME
java_home=$JAVA_HOME

neo4j_path=${neo4j_path%/}
java_home=${java_home%/}

export JAVA_HOME=$java_home
export PATH=$PATH:$java_home

_dir=$('pwd')

if [ ! -d output ] ; then
  echo -e "\n Creating output directory..."
  mkdir -p output
fi

if [ ! -d run-logs ] ; then
  echo -e "\n Creating run-logs directory..."
  mkdir -p run-logs
fi

echo -e "\nStarting Neo4j server..."
${neo4j_path}/bin/neo4j start > run-logs/neo4j.log 2>&1
#allow time for Neo4j to start
sleep $neo4j_delay_in_sec

echo -e "\n"
(cd ${source_location}/dphe-cli/target/deepphe-0.5.0-bin/deepphe-0.5.0 && java -Xmx16g -Xms8g -classpath ./resources/:./lib/* org.apache.ctakes.core.pipeline.PiperFileRunner -p pipeline/DeepPhe.piper -i $1 -o $_dir/output -r bolt://localhost:7687 --user neo4j --pass neo4jpass)

rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] DeepPhe failed. The script will now exit."
  exit $rc
else
  echo -e "...Successfully ran DeepPhe!\n" 
fi

