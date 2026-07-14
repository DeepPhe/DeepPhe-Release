#!/usr/bin/env bash

#   Starts the DeepPhe Desktop GUI.
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

# Only set DEEPPHE_ROOT if not already set
[ -z "$DEEPPHE_ROOT" ] && DEEPPHE_ROOT=`cd "$PRGDIR" >/dev/null; pwd`
export DEEPPHE_ROOT
# Only set DEEPPHE_HOME if not already set
[ -z "$DEEPPHE_HOME" ] && DEEPPHE_HOME=$DEEPPHE_ROOT/.DeepPhe
export DEEPPHE_HOME

CLASS_PATH=$DEEPPHE_HOME/resources/:$DEEPPHE_HOME/lib/*
LOG4J_PARM=-Dlog4j.configuration=file:$DEEPPHE_HOME/resources/log4j.xml
DEEPPHE_DESKTOP=org.healthnlp.deepphe.gui.DpheDesktop

cd $DEEPPHE_ROOT

java -cp $CLASS_PATH $LOG4J_PARM $DEEPPHE_DESKTOP "$@"
