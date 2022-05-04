#!/usr/bin/bash

CLIENT=client.sh
DIR=$(dirname $0)/$CLIENT
$DIR list $@
