@echo off
echo Building the DeepPhe Pipeline library, please wait ...
echo "%MAVEN_HOME%\bin\mvn" -DskipTests -f ..\..\dphe-stream/pom.xml clean compile install
"%MAVEN_HOME%\bin\mvn" -DskipTests -f ..\..\dphe-stream/pom.xml clean compile install >logs\mavenLog.txt 2>logs\errorLog.txt
