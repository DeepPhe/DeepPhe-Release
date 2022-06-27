@echo off

rem Check java version.

"%JAVA_HOME%\bin\java" -version 2> temp\java_version.txt
set /P JAVA_VER=<temp\java_version.txt

rem if the previous command could not execute then we may have a premature exit.
if not "%JAVA_VER:1.8.=good%"=="%JAVA_VER%" goto correct_java_version

:wrong_java_version
echo The java referenced in JAVA_HOME is %JAVA_VER%
echo You must be using a 64 bit Java of version 8
call getJava8home.bat
if not defined JAVA_HOME goto no_java8_home

rem Check java version again.
"%JAVA_HOME%\bin\java" -version 2> temp\java_version.txt
set /P JAVA_VER=<temp\java_version.txt
if not %JAVA_VER:1.8.=good%==%JAVA_VER% goto correct_java_version

:no_java8_home
set JAVA_HOME=""
goto end

:correct_java_version
echo Using %JAVA_VER%
goto end


:end
