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
SPLASH=resources/org/healthnlp/cancer/image/healthnlp_cancer.png

PIPE_RUNNER=org.apache.ctakes.gui.pipeline.PiperRunnerGui
PIPER_FILE=data/pipeline/DeepPhe.piper
# PIPER_CLI=PatientX.piper_cli

cd $DEEPPHE_HOME

# java -cp $DEEPPHE_HOME/data/:$DEEPPHE_HOME/lib/* -Xms512M -Xmx3g -splash:$SPLASH $PIPE_RUNNER -p $PIPER_FILE -c $PIPER_CLI "$@"
java -cp $DEEPPHE_HOME/data/:$DEEPPHE_HOME/lib/* -Xms512M -Xmx3g -splash:$SPLASH $PIPE_RUNNER -p $PIPER_FILE "$@"
