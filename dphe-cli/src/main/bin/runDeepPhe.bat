::   Starts the Piper File Submitter GUI.
::::
:: Requires JAVA JDK 1.8+
::

@REM Guess DEEPPHE_HOME if not defined
set CURRENT_DIR=%cd%
if defined DEEPPHE_HOME goto gotHome
set DEEPPHE_HOME=%CURRENT_DIR%
if exist "%DEEPPHE_HOME%\bin\runDeepPheGui.bat" goto okHome
cd ..
set DEEPPHE_HOME=%cd%

:gotHome
if exist "%DEEPPHE_HOME%\bin\runDeepPheGui.bat" goto okHome
echo The DEEPPHE_HOME environment variable is not defined correctly.  It is currently %DEEPPHE_HOME%
echo This environment variable is needed to run this program.
goto end

:okHome
@REM use JAVA_HOME if set
if exist "%JAVA_HOME%\bin\java.exe" set PATH=%JAVA_HOME%\bin;%PATH%

echo To use this script you must have use the following Parameters (-i,o,-r,--user,--pass):
echo   InputDirectory (-i)     The directory containing clinical notes.
echo   OutputDirectory (-o)    The directory to which output should be written.
echo   StartNeo4j (-n)         Location of the Neo4j installation.
echo                           Leave this blank if you do not wish to auto-start Neo4j.
echo                           If you use this option then the neo4j server 
echo                             will remain active after the pipeline ends.
echo   Neo4jUri (-r)           URI for the Neo4j Server.  
echo                           Normally bolt://127.0.0.1:7687
echo   Neo4jUser (--user)      The username for Neo4j.  
echo                           Normally "neo4j".
echo   Neo4jPass (--pass)      The password for Neo4j.
echo                           Normally "neo4j" until you change it.
echo Example: runDeepPhe -i path/to/myFiles -o put/my/output -r bolt://127.0.0.1:7687 --user neo4j --pass neo4j

cd "%DEEPPHE_HOME%"
set "CLASS_PATH=%DEEPPHE_HOME%\resources\;%DEEPPHE_HOME%\lib\*"
set LOG4J_PARM=-Dlog4j.configuration="file:\%DEEPPHE_HOME%\resources\log4j.xml"
set PIPE_RUNNER=org.apache.ctakes.core.pipeline.PiperFileRunner
set PIPER_FILE=resources/pipeline/DeepPhe.piper
java -cp "%CLASS_PATH%" %LOG4J_PARM% -Xms512M -Xmx3g %PIPE_RUNNER% -p %PIPER_FILE% %*
cd %CURRENT_DIR%

:end
