@echo off
echo Building the DeepPhe Ontology database, please wait ...
echo "%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-onto-db/pom.xml clean compile install
"%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-onto-db/pom.xml clean compile install >logs\mavenLog.txt 2>logs\errorLog.txt
