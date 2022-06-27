#!/usr/bin/env bash

echo -e "\n"
echo "**************************************************************************"
echo "* This script will build the DeepPhe system from source using Apache     *"
echo "* Maven.                                                                 *"
echo "*                                                                        *"
echo "* Copyright 2022, Boston Childrens Hospital, University of Pittsburgh    *"
echo "*                                                                        *"
echo "* Script Author: John Levander jdl50@pitt.edu                            *"
echo "**************************************************************************"

filename="installer.conf"
neo4j_delay_in_sec=5
neo4j_home_key=NEO4J_HOME
source_location="../../"

buildProject() {
  echo "   Building $1..."
  mvn -f ${source_location}$1/pom.xml clean install -U -DskipTests > build-logs/$1.log
  rc=$?
  if [ $rc -ne 0 ] ; then
    echo "      Error: please see ../build-logs/$1.log for details."
    exit $rc
  else
    echo "      Success!"
  fi
}

mkdir -p build-logs

echo "Building DeepPhe from source..."

buildProject dphe-onto-db
buildProject dphe-neo4j
buildProject dphe-neo4j-plugin
buildProject dphe-core
buildProject dphe-stream
buildProject dphe-cli

