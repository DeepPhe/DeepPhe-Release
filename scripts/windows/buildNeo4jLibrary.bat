@echo off
echo Building the DeepPhe Neo4j library, please wait ...
echo "%MAVEN_HOME%\bin\mvn" -DskipTests -f ..\..\dphe-neo4j/pom.xml clean compile install
"%MAVEN_HOME%\bin\mvn" -DskipTests -f ..\..\dphe-neo4j/pom.xml clean compile install >logs\mavenLog.txt 2>logs\errorLog.txt
