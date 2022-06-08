#!/usr/bin/bash

CLIENT=client.sh
EXE=$(dirname $0)/$CLIENT
THIS=$(basename $0)
COMMAND="${THIS%%.*}"
$EXE $COMMAND "$@"
