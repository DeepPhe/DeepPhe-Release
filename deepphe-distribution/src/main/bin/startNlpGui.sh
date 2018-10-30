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

cd $DEEPPHE_HOME
#  org/apache/ctakes/cancer/pipeline/DeepPhe.piper
java -cp $DEEPPHE_HOME/data/:$DEEPPHE_HOME/lib/* -Xms512M -Xmx3g org.apache.ctakes.gui.pipeline.PiperRunnerGui -p org/apache/ctakes/cancer/pipeline/DeepPhe.piper "$@"
