@echo off
echo Building the DeepPhe Neo4j plugin, please wait ...
echo "%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-neo4j-plugin/pom.xml clean compile package
"%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-neo4j-plugin/pom.xml clean compile package >logs\mavenLog.txt 2>logs\errorLog.txt
