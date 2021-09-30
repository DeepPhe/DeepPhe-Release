## Installing on Windows

This file contains instructions for developers that want to run DeepPhe on Windows systems.

## Prerequisites
1. A recent version of the Java 8 JDK.  Both the Oracle and Zulu OpenJDK distribution has been tested, but we can only officialy support the Oracle JDK at this time.
2. A Maven installation, version 3.6 or earlier, configured to use the Java 8 JDK.  [Maven Download Page](https://maven.apache.org/download.cgi)
3. The Git command line client. [Git Install Instructions](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
4. Privlidgnes to create directories and run 3rd party software on your machine.

## Instructions
1. Install Neo4j Community 3.5.12
   1. Windows: https://go.neo4j.com/download-thanks.html?edition=community&release=3.5.12&flavour=winzip

2. Run Build.bat, which will have output similar to the following if things run successfully:

`******************************************************`

`**    Welcome to the DeepPhe Windows setup Tool.    **`

`******************************************************`

2a. If the script can not locate JAVA_HOME, you will be prompted for the location.


`Checking Java installation ...`

`Using Java installation at C:\Program Files\Zulu\zulu-8\`

`Checking Java version for C:\Program Files\Zulu\zulu-8\\bin\java.exe ...`

`Using openjdk version "1.8.0_302"`

2b. If the script can not find the Maven software, you will be prompted for the location.

`Checking Maven installation ...`

`Creating the DeepPhe binary installation ...`

`The ontology.db directory in dphe-onto-db\src\main\resources\graph\neo4j must be copied to the neo4j server's databases directory.`

`Building the DeepPhe Ontology database, please wait ...`

`Building the DeepPhe Neo4j library, please wait ...`

`Building the DeepPhe Core library, please wait ...`

`Building the DeepPhe Pipeline library, please wait ...`

`Building the DeepPhe Run library, please wait ...`

2c. You will be prompted for an installation location for DeepPhe

` Please enter a location where you would like to place your DeepPhe binary installation.`

`DeepPhe binary installation destination: c:\users\jdl50\dev\DeepPhe`

`Copying the DeepPhe binary installation to c:\users\jdl50\dev\DeepPhe, please wait ...`

`204 File(s) copied`

`Copying the log4j configuration to c:\users\jdl50\dev\deepphetry2\config, please wait ...`

`        1 file(s) copied.`

`        1 file(s) copied.`

`Building the DeepPhe Neo4j plugin, please wait ...`

`Checking Neo4j installation ...`

2d. If the script can not find the Neo4j installation, you will be prompted for the install directory.

`Please enter the location of your Neo4j 3.5 installation`

`For instance: C:\tools\neo4j-community-3.5.12`

`Neo4j installation: c:\Users\jdl50\dev\neo4j-community-3.5.12`

`Using Neo4j installation at c:\Users\jdl50\dev\neo4j-community-3.5.12`

`Copying the DeepPhe Ontology database to c:\Users\jdl50\dev\neo4j-community-3.5.12\data\databases, please wait ...`

`0 File(s) copied`

`32 File(s) copied`

`Copying the DeepPhe Ontology database configuration to c:\Users\jdl50\dev\neo4j-community-3.5.12\conf, please wait ...`

`1 File(s) copied`

`2 File(s) copied`

`Copying the DeepPhe Neo4j plugin to c:\Users\jdl50\dev\neo4j-community-3.5.12\plugins, please wait ...`

`1 File(s) copied`

`Deleted file - c:\Users\jdl50\dev\neo4j-community-3.5.12\plugins\README.txt`

`        1 file(s) copied.`

2e.  You will be given the choice to start the Neo4J server.  Select y

`Would you like to start the Neo4j Server (y/n) y`

2f. You will be gven the choice to start the Neo4J server.  Select y

`Would you like to start the DeepPhe Job Submitter GUI (y/n) y`

