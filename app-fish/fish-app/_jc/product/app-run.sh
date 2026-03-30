#!/bin/sh

WD=`dirname $0`
CMDNAME=`basename "$0"`

# in JC_JVM java parameters -Dxxx=yyy

CP=${WD}/lib:${WD}/lib/*

JVM=
JVM="${JVM} -cp ${CP}"
JVM="${JVM} -Djandcode.app.appdir=${WD}"
JVM="${JVM} -Djandcode.app.cmdname=${CMDNAME}"
JVM="${JVM} -Dfile.encoding=UTF-8"
MAIN=fish.app.main.Main

java ${JVM} ${JC_JVM} ${MAIN} $*
