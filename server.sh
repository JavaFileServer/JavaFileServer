#!/usr/bin/bash

JAR="my-app-1.0-SNAPSHOT.jar"
JDIR=target/
DIR=$(dirname $0)/$JDIR/$JAR
java -jar $DIR "$@"
