# deepphe-viz-neo4j v0.3.0

The `DeepPhe` system extracts information from the patient cancer reports and stores the data in Neo4j graph database. The `DeepPhe-Viz` tool represents the extracted information in an organized workflow to end users, enabling exploration and discovery of patient data. This `deepphe-viz-neo4j` module creates extended Neo4j functions as plugin jars that are being used by the `DeepPhe-Viz` to query the Neo4j database.

## Installing

To build the plugin jars, you'll just do `mvn clean package` in the root directory of the DeepPhe system. You'll have a file named `deepphe-viz-0.3.0-plugin.zip` in the directory `deepphe-viz-neo4j/target` after building. This compressed file contains a directory named `plugins`. All the jar files of the plugins directory must be copied to <NEO4J_HOME>/plugins directory. Then the `DeepPhe-Viz` uses these libraries to interact with the customized DeepPhe system database.

## Neo4j Functions API Reference

The following DeepPhe-specific neo4j functions can be called directly within a Cypher query. In Neo4j browser, you can use `CALL dbms.functions()` to list all the functions including the following ones. Calling a function in Cypher looks like `return deepphe.getCohortData()`.

### deepphe.getCohortData

This function returns a list of all patients (patient properties and stages).

### deepphe.getDiagnosis

This function returns a list of diagnoses per patient for a given list of patient IDs.

### deepphe.getBiomarkers

This function returns biomarkers information for a given list of patient IDs.

### deepphe.getPatientInfo

This function returns patient properties as a map for a given pateint ID.

### deepphe.getCancerAndTumorSummary

This function returns patient cancer and tumor information for a given patient ID.

### deepphe.getTimelineData

This function returns the patient information and all the reports/notes for a given patient ID.

### deepphe.getReport

This function returns report text and all text mentions for a given report ID.

### deepphe.getFact

This function returns fact information as json for a given fact ID.