#!/usr/bin/bash

JAR="JavaFileServer-1.0-SNAPSHOT.jar"
JDIR=target/
DIR=$(dirname $0)/$JDIR/$JAR
java -jar $DIR "$@"
