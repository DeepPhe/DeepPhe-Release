#!/usr/bin/env bash

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

# Only set DEEPPHE_HOME if not already set
[ -z "$DEEPPHE_HOME" ] && DEEPPHE_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

CLASS_PATH=$DEEPPHE_HOME/resources/:$DEEPPHE_HOME/lib/*
LOG4J_PARM=-Dlog4j.configuration=file:$DEEPPHE_HOME/resources/log4j.xml

PIPE_RUNNER=org.apache.ctakes.core.pipeline.PiperFileRunner
PIPER_FILE=resources/pipeline/DeepPhe.piper

echo "To use this script you must have use the following Parameters (-i,o,-r,--user,--pass):"
echo "  InputDirectory (-i)     The directory containing clinical notes."
echo "  OutputDirectory (-o)    The directory to which output should be written."
echo "  StartNeo4j (-n)         Location of the Neo4j installation."
echo "                          Leave this blank if you do not wish to auto-start Neo4j."
echo "                          If you use this option then the neo4j server "
echo "                            will remain active after the pipeline ends."
echo "  StopNeo4j (-e)          If the value is yes then the Neo4j service will be stopped."
echo "                          This will be ignored if StartNeo4j is not used."
echo "  Neo4jUri (-r)           URI for the Neo4j Server."
echo "                          Normally bolt://127.0.0.1:7687"
echo "  Neo4jUser (--user)      The username for Neo4j."
echo "                          Normally neo4j."
echo "  Neo4jPass (--pass)      The password for Neo4j."
echo "                          Normally neo4j until you change it."
echo "Example: runDeepPhe -i path/to/myFiles -o put/my/output -r bolt://127.0.0.1:7687 --user neo4j --pass neo4j"


cd $DEEPPHE_HOME

java -cp $CLASS_PATH $LOG4J_PARM -Xms512M -Xmx3g $PIPE_RUNNER -p $PIPER_FILE "$@"
# rather than check uname and try to account for emulators etc., just check for failure and retry as cygwin.
if [ $? != 0 ]; then
   CLASS_PATH=`cygpath -pw $CLASS_PATH`
   LOG4J_PARM=-Dlog4j.configuration=file:`cygpath -w $DEEPPHE_HOME`/resources/log4j.xml
   java -cp $CLASS_PATH $LOG4J_PARM -Xms512M -Xmx3g $PIPE_RUNNER -p $PIPER_FILE "$@"
fi
