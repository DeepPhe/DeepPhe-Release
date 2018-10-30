@ECHO OFF
::
::   Runs the pipeline in a piper file.
::::
:: Requires JAVA JDK 1.8+
::

@REM Guess DEEPPHE_HOME if not defined
set CURRENT_DIR=%cd%
if not "%DEEPPHE_HOME%" == "" goto gotHome
set DEEPPHE_HOME=%CURRENT_DIR%
if exist "%DEEPPHE_HOME%\bin\startDeepPhe.bat" goto okHome
cd ..
set DEEPPHE_HOME=%cd%

:gotHome
if exist "%DEEPPHE_HOME%\bin\startDeepPhe.bat" goto okHome
echo The DEEPPHE_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome
@set PATH=%PATH%;%DEEPPHE_HOME%\data
@REM use JAVA_HOME if set
if exist "%JAVA_HOME%\bin\java.exe" set PATH=%JAVA_HOME%\bin;%PATH%

set CLASS_PATH=%DEEPPHE_HOME%\desc\;%DEEPPHE_HOME%\resources\;%DEEPPHE_HOME%\lib\*
set LOG4J_PARM=-Dlog4j.configuration=file:\%DEEPPHE_HOME%\data\log4j.xml
set SPLASH=%DEEPPHE_HOME%\resources\org\healthnlp\cancer\image\healthnlp_cancer.png

set PIPE_RUNNER=org.apache.ctakes.gui.pipeline.PiperRunnerGui
set PIPER_FILE=data\pipeline\DeepPhe.piper
:: set PIPER_CLI=PatientX.piper_cli

cd %DEEPPHE_HOME%

:: java -cp "%CLASS_PATH%" %LOG4J_PARM% -Xms512M -Xmx3g -splash:%SPLASH% %PIPE_RUNNER% -p %PIPER_FILE% -c %PIPER_CLI% %*
java -cp "%CLASS_PATH%" %LOG4J_PARM% -Xms512M -Xmx3g -splash:%SPLASH% %PIPE_RUNNER% -p %PIPER_FILE% %*
cd %CURRENT_DIR%

:end
