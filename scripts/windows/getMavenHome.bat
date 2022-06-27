@echo off

rem Check on the MAVEN_HOME variable.
if defined MAVEN_HOME goto have_user_maven

rem Attempt to set MAVEN_HOME using some previous entry.
if exist user\maven_home.bat call user\maven_home.bat
if defined MAVEN_HOME goto have_user_maven

rem Allow the user to set MAVEN_HOME.
echo Please enter the location of your Apache Maven installation
echo For instance: C:\tools\apache-maven-3.5.0
set /P MAVEN_HOME=Apache Maven installation: 
if not defined MAVEN_HOME echo No value entered for Apache Maven location.

if defined MAVEN_HOME echo set "MAVEN_HOME=%MAVEN_HOME%" > user\maven_home.bat

:have_user_maven
echo Using Maven installation at %MAVEN_HOME%

:end
