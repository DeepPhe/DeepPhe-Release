@echo off
echo Building the DeepPhe Run library, please wait ...
echo "%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-cli/pom.xml clean compile package
"%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-cli/pom.xml clean compile package >logs\mavenLog.txt 2>logs\errorLog.txt
