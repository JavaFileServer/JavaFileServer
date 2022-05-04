#!/usr/bin/bash

JDIR=target/classes/
DIR=$(dirname $0)/$JDIR
java -cp $DIR it.sssupclient.app.App $@
