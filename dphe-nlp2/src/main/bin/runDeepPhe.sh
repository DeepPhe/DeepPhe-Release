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
PIPER_FILE=resources/pipeline/DefaultDeepPhe.piper

echo "To use this script you must use the following Parameters (-i,-o):"
echo "  InputDirectory (-i)     The directory containing clinical notes."
echo "  OutputDirectory (-o)    The directory to which output files should be written."

echo "Example: runDeepPhe -i path/to/myFiles -o put/my/output"


cd $DEEPPHE_HOME

java -cp $CLASS_PATH $LOG4J_PARM -Xms512M -Xmx3g $PIPE_RUNNER -p $PIPER_FILE "$@"
# rather than check uname and try to account for emulators etc., just check for failure and retry as cygwin.
if [ $? != 0 ]; then
   CLASS_PATH=`cygpath -pw $CLASS_PATH`
   LOG4J_PARM=-Dlog4j.configuration=file:`cygpath -w $DEEPPHE_HOME`/resources/log4j.xml
   java -cp $CLASS_PATH $LOG4J_PARM -Xms512M -Xmx3g $PIPE_RUNNER -p $PIPER_FILE "$@"
fi
