# How to run these scripts

## Prerequisites
1. A recent version of the Java 8 JDK
   1.  Note, these scripts have been successfully tested on (OS : Runtime):
       1. Ubuntu 20.04.2 LTS: OpenJDK Runtime Environment (build 1.8.0_292-8u292-b10-0ubuntu1~20.04-b10)
       2. macOS 11.4 (Big Sur): OpenJDK Runtime Environment Zulu11.43+55-CA (build 11.0.9.1+1-LTS)   
3. A Maven installation, configured to use the Java 8 JDK
4. The Git command line client
5. A linux/mac environment with the ability to run shell scripts from the command line and install software (i.e. neo4j)

## Instructions
1. Install Neo4j Community 3.5.12
   1. Mac/Linux:  https://go.neo4j.com/download-thanks.html?edition=community&release=3.5.12&flavour=unix
   1. Windows: https://go.neo4j.com/download-thanks.html?edition=community&release=3.5.12&flavour=winzip


1. In the build-and-run-from-scratch folder, edit the 1.sh and 2.sh scripts to point the NEO4J_HOME variable to your Neo4j installation.
   1.  e.g. Linux: NEO4J_HOME="/opt/neo4j-community-3.5.12"
   1.  e.g. Windows: NEO4J_HOME="C:\Program Files\Neo4j3.5.12"


1. Setup your local git installation to [connect to GitHub with SSH](https://docs.github.com/en/github/authenticating-to-github/connecting-to-github-with-ssh).  This saves lots of typing when cloning several repositories and GitHub is moving in the direction of requiring this anyway so this is a good time to set it up if you haven't already.

1. Change to the build-and-run-from-scratch folder and run the shell scripts in order.



# What these scripts do
Executing all of the scripts under build-and-run-from-scratch (1.sh, 2.sh...5.sh) will download all of the DeepPhe source code, build, run DeepPhe, and visualize the results.  

More specifically, each script does the following:
- 1.sh
   1.  clone and build every DeepPhe GitHub repository that is required to run DeepPhe
   2.  clone the visualizer repository
- 2.sh   
   1. build the modules that are required to run DeepPhe
   2. do not build the visualizer yet
- 3.sh 
   1. copy a clean DeepPhe Database into the Neo4j data/databases directory  
   2. copy the dphe-neo4j-plugin into the Neo4j plugins directory
   3. start Neo4j in console mode
- 4.sh
   1. unzip the DeepPhe Command Line Interface (CLI) that was built in 1.sh
   2. run the DeepPhe CLI on fake patient data
- 5.sh 
   1. download dependencies for the DeepPhe Webservice/API
   2. build then launch the DeepPhe Webservice/API on port 3001
- 6.sh
   1. download dependencies for the DeepPhe Vizualizer Web Application
   2. build then launch the Web Application on port 3000, redirect /api requests to port 3001  

# Why there is more than one script
Scripts 3.sh, 5.sh, and 6.sh will start services in the terminal (therefore taking over that terminal) so errors/debug messages are easily spotted by developers.  Once these scripts are tested/hardened will likely be combined into a single script.

# Verifying that the scripts ran correctly

After running script 6.sh, you should see a webpage that looks as follows:

![deepphe](https://user-images.githubusercontent.com/11561825/128786082-e3f427e5-a454-4ff6-9943-deeb7b58914b.png)



# Troubleshooting

1. After running 3.sh, verify that the fresh database is pre-popluated by visiting: http://120.0.0.1/7474 and logging in.  The user is "neo4j" and the password is "neo4jpass".  Once you connect, click on the database icon in the upper left hand corner and verify that you can see node labels and relationship types and not just empty values.

2.  At the end of this process you should have 3 terminals running showing any errors, make sure to check every terminal for errors.
