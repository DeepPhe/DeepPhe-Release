@echo off

rem Check on the NEO4J_HOME variable.
if defined NEO4J_HOME goto have_user_ne04j

rem Attempt to set NEO4J_HOME using some previous entry.
if exist user\neo4j_home.bat call user\neo4j_home.bat
if defined NEO4J_HOME goto have_user_ne04j

rem Allow the user to set NEO4J_HOME.
echo Please enter the location of your Neo4j 3.5 installation
echo For instance: C:\tools\neo4j-community-3.5.26
set /P NEO4J_HOME=Neo4j installation: 
if not defined NEO4J_HOME echo No value entered for Neo4j location.

if defined NEO4J_HOME echo set "NEO4J_HOME=%NEO4J_HOME%" > user\neo4j_home.bat

:have_user_ne04j
echo Using Neo4j installation at %NEO4J_HOME%

:end
