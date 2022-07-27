@echo off

echo ******************************************************
echo **                                                  **
echo **    Welcome to the DeepPhe Windows setup Tool.    **
echo **                                                  **
echo **     For information on installation and use,     **
echo **     please visit DeepPhe.github.io               **
echo **                                                  **
echo **     Note: The DeepPhe software requires:         **
echo **     - Java Software Development Kit version 8    **
echo **     - Neo4j Graph Database Server version 3.5    **
echo **                                                  **
echo **     This setup tool requires:                    **
echo **     - Apache Maven version 3.5                   **
echo **                                                  **
echo ******************************************************
echo.

rem create user and log directories
if not exist user mkdir user
if not exist logs mkdir logs
if not exist temp mkdir temp

rem Check on the JAVA_HOME variable.
echo Checking Java installation ...
call getJava8home.bat

if not defined JAVA_HOME goto no_java_home

:have_java_home
rem Check on the java executable.
if exist "%JAVA_HOME%\bin\java.exe" goto have_java_exe
echo java.exe does not exist in %JAVA_HOME%\bin
call getJava8home.bat
if not exist "%JAVA_HOME%\bin\java.exe" goto no_java_home


:have_java_exe
rem Check java version.
echo Checking Java version for %JAVA_HOME%\bin\java.exe ...
call checkJavaVersion.bat
if not defined JAVA_HOME goto no_java_home

rem Check on the MAVEN_HOME variable.
echo Checking Maven installation ...
call getMavenHome.bat
if not defined MAVEN_HOME goto no_maven_home

rem Check on the maven executable.  mvn is always present, though mvn.cmd is executed on Windows.
if exist "%MAVEN_HOME%\bin\mvn" goto have_maven_exe
echo mvn does not exist in %MAVEN_HOME%\bin
call getMavenHome.bat
if not exist "%MAVEN_HOME%\bin\mvn" goto no_maven_home

:have_maven_exe
rem Create the DeepPhe Summarizer installation.
echo Creating the DeepPhe binary installation ...
echo The ontology.db directory in ..\..\dphe-onto-db\src\main\resources\graph\neo4j must be copied to the neo4j server's databases directory.
echo Building the DeepPhe Ontology database, please wait ...
start "Building the DeepPhe Ontology database" /WAIT cmd /c buildOntologyDb.bat
if not %errorlevel%==0 goto maven_error
echo Building the DeepPhe Neo4j library, please wait ...
start "Building the DeepPhe Neo4j library" /WAIT cmd /c buildNeo4jLibrary.bat
if not %errorlevel%==0 goto maven_error
echo Building the DeepPhe Core library, please wait ...
start "Building the DeepPhe Core library" /WAIT cmd /c buildCoreLibrary.bat
if not %errorlevel%==0 goto maven_error
echo Building the DeepPhe Pipeline library, please wait ...
start "Building the DeepPhe Pipeline library" /WAIT cmd /c buildStreamLibrary.bat
if not %errorlevel%==0 goto maven_error
echo Building the DeepPhe Run library, please wait ...
start "Building the DeepPhe Run library" /WAIT cmd /c buildCliLibrary.bat
if not %errorlevel%==0 goto maven_error


rem Move the binary installation.
echo.
echo  Please enter a location where you would like to place your DeepPhe binary installation.
set /P DEEPPHE_BIN=DeepPhe binary installation destination: 
if not defined DEEPPHE_BIN (
   echo No value entered.  You can find the binary installation in ..\..\dphe-cli\target\deepphe-0.5.0-bin
   goto create_plugin
   )
echo Copying the DeepPhe binary installation to %DEEPPHE_BIN%, please wait ...
xcopy ..\..\dphe-cli\target\deepphe-0.5.0-bin\* "%DEEPPHE_BIN%" /Q /S /E /I /Y
if not %errorlevel%==0 echo The DeepPhe binary at ..\..\dphe-cli\target\deepphe-0.5.0-bin could not be copied to %DEEPPHE_BIN%
echo Copying the log4j configuration to %DEEPPHE_BIN%\config, please wait ...
if not exist "%DEEPPHE_BIN%\deepphe-0.5.0\config" mkdir "%DEEPPHE_BIN%\deepphe-0.5.0\config"
copy /Y ..\..\dphe-core\src\main\resources\log4j.properties "%DEEPPHE_BIN%\deepphe-0.5.0\config"
copy /Y ..\..\dphe-core\src\main\resources\log4j.xml "%DEEPPHE_BIN%\deepphe-0.5.0\config"


:create_plugin
rem Create the DeepPhe Neo4j plugin.
echo Building the DeepPhe Neo4j plugin, please wait ...
start "Building the DeepPhe Neo4j plugin" /WAIT cmd /c buildNeo4jPlugin.bat
if not %errorlevel%==0 goto maven_error

rem Check on the NEO4J_HOME variable.
echo Checking Neo4j installation ...
call getNeo4jHome.bat
if not defined NEO4J_HOME goto no_neo4j_home

rem Check on the neo4j executable.
if exist "%NEO4J_HOME%\bin\neo4j.bat" goto have_neo4j_bat
echo neo4j.bat does not exist in %NEO4J_HOME%\bin
call getNeo4jHome.bat
if not exist "%NEO4J_HOME%\bin\neo4j.bat" goto no_neo4j_home

:have_neo4j_bat
rem Copy DeepPhe neo4j resources to the neo4j installation.
echo Copying the DeepPhe Ontology database to %NEO4J_HOME%\data\databases, please wait ...
if not exist "%NEO4J_HOME%\pre_deepphe_data" xcopy "%NEO4J_HOME%\data" "%NEO4J_HOME%\pre_deepphe_data" /Q /S /E /I /Y
xcopy ..\..\dphe-onto-db\src\main\resources\graph\neo4j\* "%NEO4J_HOME%\data\databases" /Q /S /E /I /Y
if not %errorlevel%==0 echo The DeepPhe Ontology database at ..\..\dphe-onto-db\src\main\resources\graph\neo4j could not be copied to %NEO4J_HOME%\data\databases

echo Copying the DeepPhe Ontology database configuration to %NEO4J_HOME%\conf, please wait ...
if not exist "%NEO4J_HOME%\pre_deepphe_conf" xcopy "%NEO4J_HOME%\conf" "%NEO4J_HOME%\pre_deepphe_conf" /Q /S /E /I /Y
xcopy ..\..\dphe-neo4j-plugin\conf\* "%NEO4J_HOME%\conf" /Q /S /E /I /Y
if not %errorlevel%==0 echo The DeepPhe Neo4j configuration at dphe-neo4j-plugin\conf could not be copied to %NEO4J_HOME%\conf

echo Copying the DeepPhe Neo4j plugin to %NEO4J_HOME%\plugins, please wait ...
if not exist "%NEO4J_HOME%\pre_deepphe_plugins" xcopy "%NEO4J_HOME%\plugins" "%NEO4J_HOME%\pre_deepphe_plugins" /Q /S /E /I /Y
erase /S /F /Q "%NEO4J_HOME%\plugins\*.*"
copy /Y ..\..\dphe-neo4j-plugin\target\deepphe-neo4j-plugin-0.5.0.jar "%NEO4J_HOME%\plugins"
if not %errorlevel%==0 echo The DeepPhe Neo4j plugin ..\..\dphe-neo4j-plugin\target\deepphe-neo4j-plugin-0.5.0.jar could not be copied to %NEO4J_HOME%\plugins

rem Start the Neo4j Server.
set /P START_NEO4J=Would you like to start the Neo4j Server (y/n) 
if not %START_NEO4J%==y goto start_gui
start "Running the Neo4j server" cmd /c "%NEO4J_HOME%\bin\neo4j.bat console"

:start_gui
rem Start the Pipeline GUI.
set /P START_GUI=Would you like to start the DeepPhe Job Submitter GUI (y/n) 
if not %START_GUI%==y goto end
set "DEEPPHE_HOME=%DEEPPHE_BIN%\deepphe-0.5.0"
cd "%DEEPPHE_HOME%"
call bin\runDeepPheGui.bat


goto end

:maven_error
echo Some error occurred while attempting to execute a maven process:
type logs\errorLog.txt
type logs\mavenLog.txt
goto end

:no_java_home
echo Please install a 64 bit version of Java 8 development kit (jdk) and/or check the location and rerun this script.
echo If you do not have a 64 bit Java 8 jdk installation, please visit https://www.oracle.com/java/technologies/javase-jre8-downloads.html
echo DeepPhe requires Java 8.  It will not run on newer versions of Java.
goto end

:no_maven_home
echo Please install Apache Maven and/or check the location and rerun this script.
echo If you do not have an Apache Maven installation, please visit https://maven.apache.org/download.cgi
goto end

:no_neo4j_home
echo Please install Neo4j 3.5 and/or check the location and rerun this script.
echo If you do not have a Neo4j 3.5 installation, please visit https://neo4j.com/download-center/
echo DeepPhe requires Neo4j 3.5.  It will not run on newer versions of Neo4j.
goto end


:end
