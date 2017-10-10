#!/usr/bin/env bash

#
#   Runs the Document summarization pipeline.
#
# Requires JAVA JDK 1.8+
#

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

cd $DEEPPHE_HOME
CLASS_PATH=$DEEPPHE_HOME/data/:$DEEPPHE_HOME/resources/:$DEEPPHE_HOME/lib/*
LOG4J_PARM=-Dlog4j.configuration=file:\$DEEPPHE_HOME\data\log4j.xml
PIPE_RUNNER=org.apache.ctakes.gui.pipeline.PiperRunnerGui
java -cp $CLASS_PATH $LOG4J_PARM -Xms512M -Xmx3g $PIPE_RUNNER -p data/pipeline/Phenotype.piper "$@"
