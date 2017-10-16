# Cancer Deep Phenotype Extraction (DeepPhe), Software Release

## Prerequisites

You must have the following tools installed to build this project:

- [Oracle Java SE Development Kit 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3.x](https://maven.apache.org/download.cgi)
- [neo4j 3.2.1](https://neo4j.com/) to store graph database output

The following tools are recommended
- [IntelliJ](https://www.jetbrains.com/idea/) IDE is suggested to run the pipeline
- [Eclipse](https://eclipse.org/ide/) is another IDE that can be used to run the pipeline

## Obtaining the code

There are several methods that can be used to obtain the DeepPhe code from GitHub.
The three most common are:
1. Download using a browser a Zip file from [GitHub](https://github.com/DeepPhe/DeepPhe-Release/)
    1. [Download the Zip File](https://github.com/DeepPhe/DeepPhe-Release/archive/master.zip)
    2. Unzip the file
2. Clone using [Git](https://git-scm.com/)
    ````
    git clone https://github.com/DeepPhe/DeepPhe-Release.git
    ````
3. Checkout using [Subversion](https://subversion.apache.org/)
    ````
    svn co https://github.com/DeepPhe/DeepPhe-Release.git
    ````

## DeepPhe file structure

The DeepPhe root directory contains four Maven modules:
1. deepphe-ctakes-cancer
    - Code and resources to extend [cTAKES](https://ctakes.apache.org)
2. deepphe-distribution
    - Code and resources to create a binary installation
3. deepphe-fhir
    - Code to create [FHIR](https://www.hl7.org/fhir/) output
4. deepphe-uima
    - Code and resources to perform Document and Phenotype summarization

There is also a ``data/`` directory with subdirectories containing:
1. ``data/ontology/`` 
    - two cancer ontologies:
        1. Breast Cancer
        2. Melanoma
2. ``data/sample/reports/patientX/`` 
    - three example notes for a fictional patientX
3. ``data/pipeline/`` 
    - configuration files to run the pipelines

## Running the DeepPhe software

There are two main pipelines in the DeepPhe workflow:
1. Document Summarization
2. Phenotype Summarization

### Document Summarization
Document Summarization is run using a [Maven Profile](http://maven.apache.org/guides/introduction/introduction-to-profiles.html).

There are two predefined Document Summarization profiles:
1. startBreastCancer
2. startMelanoma

Each Profile will start a [Piper File Submitter GUI](https://cwiki.apache.org/confluence/display/CTAKES/Piper+File+Submitter+GUI) 
with default settings to run notes for [patientX](#deepphe-file-structure) with the appropriate ontology.
These settings include:
- Input Directory (with Notes) *
- Output Directory (for Document Summary files) *
- Lookup Xml (with Ontology parameters)
- Ontology Uri (for non-default specification)
- Sections Owl (for Summarization)

Unless you are using a custom ontology, you only need to change the Input and Output directories.

The GUI will allow you to load different pipelines, load and save custom pipeline parameters, and run pipelines.
See [Piper File Submitter GUI](https://cwiki.apache.org/confluence/display/CTAKES/Piper+File+Submitter+GUI) for more information.

Profiles can be run from the command line:
````
mvn compile -PstartBreastCancer
````
or using an IDE with Maven integration such as [IntelliJ](https://www.jetbrains.com/help/idea/maven.html#use_profiles_maven)

### Phenotype Summarization
Phenotype Summarization is run using a [Maven Profile](http://maven.apache.org/guides/introduction/introduction-to-profiles.html).

There are two predefined Phenotype Summarization profiles:
1. startBreastCancerPhe
2. startMelanomaPhe

Each Profile will start a [Piper File Submitter GUI](https://cwiki.apache.org/confluence/display/CTAKES/Piper+File+Submitter+GUI) 
with default settings to run notes for [patientX](#deepphe-file-structure) with the appropriate ontology.
These settings include:
- Input Directory (Document Summarization Output Directory) *
- Output Directory (for Phenotype Summary files) *
- Configuration Directory (with TranSMART files)
- TCGA ID Mapping File (for tcga mapping)
- Sections Owl (for Summarization)

Unless you are using a custom ontology, you only need to change the Input and Output directories.

The GUI will allow you to load different pipelines, load and save custom pipeline parameters, and run pipelines.
See [Piper File Submitter GUI](https://cwiki.apache.org/confluence/display/CTAKES/Piper+File+Submitter+GUI) for more information.

Profiles can be run from the command line:
````
mvn compile -PstartBreastCancerPhe
````
or using an IDE with Maven integration such as [IntelliJ](https://www.jetbrains.com/help/idea/maven.html#use_profiles_maven)


## Building a binary zip file

````
mvn clean package -Dmaven.test.skip=true
````
	
If you are using a binary installation of DeepPhe, you can run pipelines using a gui by executing scripts from the command line:
````
bin/DocumentSummarizer
bin/PhenotypeSummarizer
````
There are example run configurations in ``data/pipeline/``, stored in `.piper_cli` files that can be loaded by the gui.
	
## Load data to neo4j

Run the phenotype level pipeline at least once so you have a [neo4jdb](https://neo4j.com/) folder generated at `DeepPhe/data/sample/output`.

Then start neo4J 3.2.1 Server and point it to the neo4jdb folder. Neo4j 3.2.1 uses `NEO4J_HOME/data/databases` to store all the databases. Here we create a symbolic link:

````
ln -s /path_to/DeepPhe/data/sample/output /path_to/neoj4j-3.2.1/data/databases/deepphe.db
````

You'll also need to edit the `conf/neo4j.conf` to point to this `deepphe.db`:

````
dbms.active_database=deepphe.db
````

Then start the neo4j database server:
````
cd neo4j_home_directory
./bin/neo4j start
````

Once the neo4j server is running, you should be able to access the Neo4j browser at http://localhost:7474/ and explore the generated neo4j database, execute Cypher queries and see results in tabular or graph form.
