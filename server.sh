#!/usr/bin/bash

JDIR=target/classes/
DIR=$(dirname $0)/$JDIR
java -cp $DIR it.sssupserver.app.App $@
