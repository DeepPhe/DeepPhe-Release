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

CLASS_PATH=$DEEPPHE_HOME/resources/:$DEEPPHE_HOME/data/:$DEEPPHE_HOME/lib/*
PIPE_RUNNER=org.apache.ctakes.gui.pipeline.PiperRunnerGui
PIPER_FILE=data/pipeline/DeepPhe.piper

cd $DEEPPHE_HOME

java -cp $CLASS_PATH -Xms512M -Xmx3g $PIPE_RUNNER -p $PIPER_FILE "$@"
# rather than check uname and try to account for emulators etc., just check for failure and retry.
if [ $? != 0 ]; then
   java -cp ${CLASS_PATH//\//\\} -Xms512M -Xmx3g $PIPE_RUNNER -p $PIPER_FILE "$@"
fi
