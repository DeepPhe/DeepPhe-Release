::   Starts the DeepPhe Desktop GUI.
::
:: Requires JAVA JDK 1.8+
::

set CURRENT_DIR=%cd%

@REM Guess DEEPPHE_ROOT if not defined
if defined DEEPPHE_ROOT goto gotRoot
set DEEPPHE_ROOT=%CURRENT_DIR%
if exist "%DEEPPHE_ROOT%\.DeepPhe\bin\runDeepPheGui.bat" goto okRoot
cd ..
set DEEPPHE_ROOT=%cd%

:gotRoot
if exist "%DEEPPHE_ROOT%\.DeepPhe\bin\runDeepPheGui.bat" goto okRoot
echo The DEEPPHE_ROOT environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okRoot
@REM use JAVA_HOME if set
if exist "%JAVA_HOME%\bin\java.exe" set PATH=%JAVA_HOME%\bin;%PATH%

if defined DEEPPHE_HOME goto gotHome
set DEEPPHE_HOME=%DEEPPHE_ROOT%\.DeepPhe

:gotHome
if exist "%DEEPPHE_HOME%\bin\runDeepPheGui.bat" goto okHome
echo The DEEPPHE_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome

set "CLASS_PATH=%DEEPPHE_HOME%\resources\;%DEEPPHE_HOME%\lib\*"
set LOG4J_PARM=-Dlog4j.configuration="file:\%DEEPPHE_HOME%\config\log4j.xml"
DEEPPHE_DESKTOP=org.healthnlp.deepphe.gui.DpheDesktop

cd "%DEEPPHE_ROOT%"

java -cp "%CLASS_PATH%" %LOG4J_PARM% %DEEPPHE_DESKTOP% %*
cd %CURRENT_DIR%

:end
