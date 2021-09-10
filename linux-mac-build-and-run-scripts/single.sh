#!/bin/bash

filename="installer.conf"

neo4j_home_key=NEO4J_HOME

buildProject() {
echo "   Building $1..."
mvn -f ../$1/pom.xml clean install -U -DskipTests > logs/$1.log
rc=$?
if [ $rc -ne 0 ] ; then
  echo "      Error: please see logs/dphe-onto-db.log for details."
  exit $rc
else
  echo "      Success!"
fi
}

handleError() {
if [ $? -ne 0 ]
then
    echo "    Error: [$rc]"; exit $rc
else
    echo "    Success!"
fi
}

echo -e "\n"
echo "**************************************************************************"
echo "* Welcome to the DeepPhe install script for MacOS and Linux systems.  If *"
echo "* this is your first time running, you will be prompted for the location *"
echo "* of your Neo4j installation directory.                                  *"
echo "*                                                                        *"
echo "* Note: This project requires the Neo4j Graph Database Platform, the     *"
echo "* Apache Maven project majagement tool, and the Git software tracking    *"
echo "* software in order to build the project from the source code.           *"
echo "*                                                                        *"                                           
echo "* If you do not have all of these scripts installed, please cancel this  *"
echo "* script and refer to the README.md file in this directory for download  *"
echo "* instructions.                                                          *"
echo "*                                                                        *"
echo "* Copyright 2021, Boston Childrens Hospital, University of Pittsburgh,   *"
echo "*   and Vanderbilt University Medical Center                             *"                           
echo "*                                                                        *"
echo "* Script Author: John Levander jdl50@pitt.edu                            *"
echo "**************************************************************************"


#####
##### STEP 1 - DETERMINE NEO4J HOME DIRECTORY AND VALIDATE INSTALLATION #####
#####

mkdir -p logs

echo -e "\nReading configuration file..."
#if the NEO4J_HOME key is not set in the installer_config.txt file
if ! grep -R --no-messages "^[#]*\s*${neo4j_home_key}=.*" $filename > /dev/null; then
   until [ -d $neo4j_path ] && [ ! -z "$neo4j_path" ] ; do
       #ask the user to specify the NEO4J_HOME directory
       read -p "  Enter location of the Neo4j home folder (e.g. /opt/neo4j-community-3.5.12): " neo4j_path
       
       #remove trailing slash if it's there
       neo4j_path=${neo4j_path%/}

       echo "    Verifying the following directories exist: ${neo4j_path}/bin/, ${neo4j_path}/conf/, ${neo4j_path}/data"
       #check if bin directory exists
       if [ -d ${neo4j_path}/bin/ ] &&  [ -d ${neo4j_path}/conf/ ] &&  [ -d ${neo4j_path}/data/ ]  ; then
        echo "    The provided Neo4j home folder is valid."
        echo "$neo4j_home_key=$neo4j_path" >> $filename
       else
        echo "    Invalid Neo4j home folder specified.  Tip: The Neo4j home folder will contain a bin, conf, and data directory."
        unset neo4j_path
       fi
  done
else 
  #load the propertis file
  . ${filename}
  neo4j_path=$NEO4J_HOME
  echo -e "  Neo4j location already configured to: $neo4j_path"
  echo "    Verifying the following directories exist: ${neo4j_path}/bin/, ${neo4j_path}/conf/, ${neo4j_path}/data"
  if [ -d ${neo4j_path}/bin/ ] &&  [ -d ${neo4j_path}/conf/ ] &&  [ -d ${neo4j_path}/data/ ]  ; then
        echo -e "    The provided Neo4j home folder is valid. \n"
  else
    echo The Neo4j location does not appear to be a working Neo4j install.
    echo Please delete the file: $filename and run this script again.
    exit 1
  fi
fi


#kill neo4j if it is running
ps -ef | grep your_process_name | grep -v grep | awk '{print $2}' | xargs kill
echo -e  "\nStarting Neo4j..."
sleep 2
${neo4j_path}/bin/neo4j start > logs/neo4j.log 2>&1
sleep 5


#####
##### STEP 2 - DETERMINE MAVEN IS INSTALLED
#####

mvn -v > /dev/null
rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error:[$rc] A working Maven installation was not found.  Please confirm that you have a working Maven installation,  exit all terminals, and re-run this script.  The script will now exit."
  exit $rc
fi

#####
##### STEP 3 - BUILD FROM SOURCE
#####


echo "Building DeepPhe from source..."


buildProject dphe-onto-db
buildProject dphe-neo4j
buildProject dphe-neo4j-plugin
buildProject dphe-core
buildProject dphe-stream
buildProject dphe-cli


#####
##### STEP 4 -- COPY OPERATIONS
#####

echo -e "\nPerforming copy operations..."
echo "  Copying a clean database from src/main/resources/graph/neo4j/ontology.db to ${neo4j_path}/data/databases..."

(rm -rf ${neo4j_path}/data/databases/ontology.db && cd ../dphe-onto-db && cp -R src/main/resources/graph/neo4j/ontology.db ${neo4j_path}/data/databases)

handleError

echo "  Copying dphe-neo4j-plugin from ../dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.4.0.jar into ${neo4j_path}/plugins..."
cp ../dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.4.0.jar  ${neo4j_path}/plugins

handleError

#####
##### STEP 5 - DOWNLOAD SAMPLE DATA
#####

#determine if git is installed
git --version > /dev/null
rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] A working Git installation was not found.  Please confirm that you have a working Git installation, exit all terminals, and re-run this script.  The script will now exit."
  exit $rc
fi

 if [ -d ${dphe-examples} ]; then
  echo "Downloading sample data..."
  git clone --depth 1 --branch v20210827 https://github.com/DeepPhe/dphe-examples.git
 else
 echo -e "\nSkipping download of sample data as directory dphe-examples exists." 
 fi

handleError

_dir=$('pwd')

if [ ! -d output ] ; then
echo -e "\n Creating output ditrectory..."
mkdir -p output
fi

#####
##### STEP 6 - RUN DEEPPHE
#####
echo -e "\n"
echo "DeepPhe is configured and is ready to run, please press enter to run DeepPhe on the reports in the dphe-examples folder: [ENTER]"
(cd ../dphe-cli/target/deepphe-0.4.0-bin/deepphe-0.4.0 && java -Xmx3g -Xms512M -classpath ./resources/:./lib/* org.apache.ctakes.core.pipeline.PiperFileRunner -p pipeline/DeepPhe.piper -i $_dir/dphe-examples/reports/ -o $_dir/output -r bolt://localhost:7687 --user neo4j --pass neo4jpass)




