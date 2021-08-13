@echo off

rem Check on the JAVA_HOME variable.
if defined JAVA_HOME goto have_user_java

rem Attempt to set JAVA_HOME using some previous entry.
if exist user\ava8_home.bat call user\java8_home.bat
if defined JAVA_HOME goto have_user_java

for /f "delims=" %%i in ('where java') do set JAVA_EXE="%%i"
if not defined JAVA_EXE goto enter_java_home
echo It appears that you have a java executable at %JAVA_EXE%
if %JAVA_EXE:bin=good%==%JAVA_EXE% goto no_java_bin
echo Attempting to utilize %JAVA_EXE%\..\.. as JAVA_HOME
echo If this is incorrect then either manually set JAVA_HOME or create a file user\java8_home.bat with the contents: set "JAVA_HOME=[your java home]"
set "JAVA_HOME=%JAVA_EXE%\..\.."
goto save_java_home

:no_java_bin
echo %JAVA_EXE% does not appear to be in a bin subdirectory.
echo It cannot be used for JAVA_HOME.

:enter_java_home
rem Allow the user to set JAVA_HOME.
echo Please enter the location of your 64 bit Java 8 development kit installation.
echo For instance: C:\Program Files\Java\jdk1.8.0_271
set /P JAVA_HOME=Java 8 development kit: 
if not defined JAVA_HOME echo No value entered for Java 8 development kit location.

:save_java_home
if defined JAVA_HOME echo set "JAVA_HOME=%JAVA_HOME%" > user\java8_home.bat

:have_user_java
echo Using Java installation at %JAVA_HOME%

:end
