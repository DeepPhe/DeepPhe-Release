@echo off
echo Building the DeepPhe Core library, please wait ...
echo "%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-core/pom.xml clean compile install
"%MAVEN_HOME%\bin\mvn" -DskipTests -f dphe-core/pom.xml clean compile install >logs\mavenLog.txt 2>logs\errorLog.txt

