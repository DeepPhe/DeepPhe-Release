::
::   Runs the Document summarization pipeline.
::::
:: Requires JAVA JDK 1.8+
::

@REM Guess DEEPPHE_HOME if not defined
set CURRENT_DIR=%cd%
if not "%DEEPPHE_HOME%" == "" goto gotHome
set DEEPPHE_HOME=%CURRENT_DIR%
if exist "%DEEPPHE_HOME%\bin\DocumentSummarizer.bat" goto okHome
cd ..
set DEEPPHE_HOME=%cd%

:gotHome
if exist "%DEEPPHE_HOME%\bin\DocumentSummarizer.bat" goto okHome
echo The DEEPPHE_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome
@REM use JAVA_HOME if set
if exist "%JAVA_HOME%\bin\java.exe" set PATH=%JAVA_HOME%\bin;%PATH%

cd %DEEPPHE_HOME%
set CLASS_PATH=%DEEPPHE_HOME%\data\;%DEEPPHE_HOME%\resources\;%DEEPPHE_HOME%\lib\*
set LOG4J_PARM=-Dlog4j.configuration=file:\%DEEPPHE_HOME%\data\log4j.xml
set PIPE_RUNNER=org.apache.ctakes.gui.pipeline.PiperRunnerGui
java -cp "%CLASS_PATH%" %LOG4J_PARM% -Xms512M -Xmx3g %PIPE_RUNNER% -p %DEEPPHE_HOME%\data\pipeline\DocumentSummarizer.piper %*
cd %CURRENT_DIR%

:end
