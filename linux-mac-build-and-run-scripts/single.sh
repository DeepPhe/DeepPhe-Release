#!/usr/bin/env bash

echo -e "\n"
echo "**************************************************************************"
echo "* Welcome to the DeepPhe install script for MacOS and Linux systems.  If *"
echo "* this is your first time running, you will be prompted for the location *"
echo "* of your Neo4j installation directory.                                  *"
echo "*                                                                        *"
echo "* Note: This project requires the Neo4j Graph Database Platform, the     *"
echo "* Apache Maven project management tool, and the Git software tracking    *"
echo "* software in order to build the project from the source code.           *"
echo "*                                                                        *"                                           
echo "* If you do not have all of these programs installed, please cancel this *"
echo "* script and refer to the README.md file in this directory for download  *"
echo "* instructions.                                                          *"
echo "*                                                                        *"
echo "* Copyright 2021, Boston Childrens Hospital, University of Pittsburgh,   *"
echo "*   and Vanderbilt University Medical Center                             *"                           
echo "*                                                                        *"
echo "* Script Author: John Levander jdl50@pitt.edu                            *"
echo "**************************************************************************"

### Constants
filename="installer.conf"
neo4j_delay_in_sec=5
neo4j_home_key=NEO4J_HOME

### Functions
buildProject() {
  echo "   Building $1..."
  #mvn -f ../$1/pom.xml clean install -U -DskipTests > logs/$1.log
  rc=$?
  if [ $rc -ne 0 ] ; then
    echo "      Error: please see logs/$1.log for details."
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

### Script Logic Start

#####
##### STEP 1 -CONFIRM PREREQUISITE PROGRAMS ARE INSTALLED
#####

#determine if mvn is installed, if not, exit
mvn -v > /dev/null
rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error:[$rc] A working Maven installation was not found.  Please confirm that you have a working Maven installation,  exit all terminals, and re-run this script.  The script will now exit."
  exit $rc
fi

#determine if git is installed, if not, exit
git --version > /dev/null
rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] A working Git installation was not found.  Please confirm that you have a working Git installation, exit all terminals, and re-run this script.  The script will now exit."
  exit $rc
fi

#determine if Neo4j is installed by prompting user for the location of the install directory, do not continue until "valid" directory is entered
echo -e "\nReading configuration file..."
#if the NEO4J_HOME key is not set in the installer_config.txt file
if ! grep -R --no-messages "^[#]*\s*${neo4j_home_key}=.*" $filename > /dev/null; then
   until [ -d $neo4j_path ] && [ ! -z "$neo4j_path" ] ; do
       #ask the user to specify the NEO4J_HOME directory
       read -p "  Enter location of the Neo4j home folder (e.g. /opt/neo4j-community-3.5.12): " neo4j_path
       
       #remove trailing slash if it's there
       neo4j_path=${neo4j_path%/}

       echo "    Verifying the following directories exist: ${neo4j_path}/bin/, ${neo4j_path}/conf/, ${neo4j_path}/data"
       if [ -d ${neo4j_path}/bin/ ] &&  [ -d ${neo4j_path}/conf/ ] &&  [ -d ${neo4j_path}/data/ ]  ; then
        echo "    The provided Neo4j home folder is valid."
        echo "$neo4j_home_key=$neo4j_path" >> $filename
       else
        echo "    Invalid Neo4j home folder specified.  Tip: The Neo4j home folder will contain a bin, conf, and data directory."
        unset neo4j_path
       fi
  done
else 
  #NEO4J_HOME is set in the properties file, so load it
  . ${filename}

  #neo4j_path varabile is used above so it will be used here for clarity
  neo4j_path=$NEO4J_HOME

  #validate neo4j install, if invalid, exit
  echo -e "  Neo4j location already configured to: $neo4j_path"
  echo "    Verifying the following directories exist: ${neo4j_path}/bin/, ${neo4j_path}/conf/, ${neo4j_path}/data"
  if [ -d ${neo4j_path}/bin/ ] &&  [ -d ${neo4j_path}/conf/ ] &&  [ -d ${neo4j_path}/data/ ]  ; then
        echo -e "    The provided Neo4j home folder is valid."
  else
    echo The Neo4j location does not appear to be a working Neo4j install.
    echo Please delete the file: $filename and run this script again.
    exit 1
  fi
fi

#kill neo4j if it is running
ps -ef | grep neo4j | grep -v grep | awk '{print $2}' | xargs kill
echo -e  "\nSending kill signal to any existing Neo4j processes...\n"
sleep 10
#####
##### STEP 2 - BUILD FROM SOURCE
#####

#Create the logs directory if it does not exist, output is redirected there
mkdir -p logs

echo "Building DeepPhe from source..."

buildProject dphe-onto-db
buildProject dphe-neo4j
buildProject dphe-neo4j-plugin
buildProject dphe-core
buildProject dphe-stream
buildProject dphe-cli

#####
##### STEP 3 -- RESOURCE COPY OPERATIONS
#####

echo -e "\nPerforming copy operations..."
echo "  Copying a clean database from src/main/resources/graph/neo4j/ontology.db to ${neo4j_path}/data/databases..."

(rm -rf ${neo4j_path}/data/databases/ontology.db && cd ../dphe-onto-db && cp -R src/main/resources/graph/neo4j/ontology.db ${neo4j_path}/data/databases)

handleError

echo "  Copying dphe-neo4j-plugin from ../dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.4.0.jar into ${neo4j_path}/plugins..."
cp ../dphe-neo4j-plugin/target/deepphe-neo4j-plugin-0.4.0.jar  ${neo4j_path}/plugins

handleError
num_deepphe_plugins=$(ls -l ${neo4j_path}/plugins/deepphe*.jar | wc -l)
if [ $num_deepphe_plugins -gt 1 ]; then
    echo -e "\nERROR: It appears there are two DeepPhe plugins in the ${neo4j_path}/plugins directory.  Please remove all DeepPhe plugins from that directory and restart this script."
    exit
fi

#####
##### STEP 4 - DOWNLOAD/VALIDATE SAMPLE DATA
#####

 if [ ! -d "./dphe-examples" ]; then
   echo "Downloading sample data..."
   git clone --depth 1 --branch v20210827 https://github.com/DeepPhe/dphe-examples.git
 else
   echo -e "\nSkipping download of sample data as directory dphe-examples exists." 
 fi

handleError

#####
##### STEP 5 - RUN DEEPPHE
#####

#get the current directory, required to run DeepPhe
_dir=$('pwd')

if [ ! -d output ] ; then
  echo -e "\n Creating output ditrectory..."
  mkdir -p output
fi

echo -e "\nStarting Neo4j server..."
${neo4j_path}/bin/neo4j start > logs/neo4j.log 2>&1

#allow time for Neo4j to start
sleep $neo4j_delay_in_sec

echo -e "\n"
read -p "DeepPhe is configured and is ready to run, please press enter to run DeepPhe on the reports in the dphe-examples folder: [ENTER]"
(cd ../dphe-cli/target/deepphe-0.4.0-bin/deepphe-0.4.0 && java -Xmx3g -Xms512M -classpath ./resources/:./lib/* org.apache.ctakes.core.pipeline.PiperFileRunner -p pipeline/DeepPhe.piper -i $_dir/dphe-examples/reports/ -o $_dir/output -r bolt://localhost:7687 --user neo4j --pass neo4jpass)

rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] DeepPhe failed. The script will now exit."
  exit $rc
else
  echo -e "...Successfully ran DeepPhe!\n" 
fi

#####
##### STEP 5 - RUN VISUALIZER
#####

### Install visualizer speicifc preprequisite: Node
echo  
 while true; do
      read -p "DeepPhe completed successfully, would you like to install or run the visualizer? [Y/N]" yn
      case $yn in
          [Yy]* ) break;;
          [Nn]* ) echo "Goodbye."; exit;;
          * ) echo "Please answer Y or N.";;
      esac
    done

## If Node is not installed, ask the user if they want to try to install it, if not, exit

echo -ne "\nDetecting Node installation..."
if  ! type -P "node" ; then
    while true; do
      read -p "Node installation not detectied, would you like this script to attempt to install it? [Y/N]" yn
      case $yn in
          [Yy]* ) break;;
          [Nn]* ) echo "Script will now exit.  Please install Node and try to run this script again"; exit;;
          * ) echo "Please answer Y or N.";;
      esac
    done
    if [ "$(uname)" == "Darwin" ]; then
        which -s brew
        if [[ $? != 0 ]] ; then
            # Install Homebrew
            ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
        else
            brew update
        fi

        #if npm is not installed, attempt to install it     
        if ! type -P "npm" ; then
          brew install npm
        fi

        npm install node
    elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
        wget -qO- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash
        nvm install -g npm
   
    fi
fi

#Download the Visualizer source code
if [ ! -d ./DeepPhe-Viz ]; then
  echo "Downloading Visualizer..."
  git clone --depth 1 --branch v20210826 https://github.com/DeepPhe/DeepPhe-Viz.git
else
  echo -e "\nSkipping download of Visualizer, Visualizer directory DeepPhe-Viz already found!\n" 
fi

echo "Building DeepPhe Webservice API..."
(cd DeepPhe-Viz/api && npm install > ../../logs/webservice-api-install.log 2>&1)

rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] when building DeepPhe Webservice API, see /logs/webservice-api-install.log for details."
  exit $rc
else
  echo "...Success!" 
fi

echo "Starting DeepPhe Webservice API..."
(export PORT=3001 && cd DeepPhe-Viz/api && npm start > ../../logs/webservice-api-run.log 2>&1 &)
rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] when starting DeepPhe Webservice API, see /logs/webservice-api-run.log for details."
  exit $rc
else
  echo "...Success!" 
fi

#unset the HOST environment variable for local installations
unset HOST

echo -e "\nBuilding DeepPhe Visualizer..."
(cd DeepPhe-Viz/client && npm install --save --legacy-peer-deps > ../../logs/visualizer-install.log 2>&1)

rc=$?
if [ $rc -ne 0 ] ; then
  echo "Error: [$rc] when building DeepPhe Visualizer, see /logs/visualizer-install.log for details."
  exit $rc
else
  echo "...Success!" 
fi

echo "Starting DeepPhe Visualizer...(Press CTRL-C to terminate)"
(cd DeepPhe-Viz/client && npm start > ../../logs/visualizer-run.log 2>&1 ) 

