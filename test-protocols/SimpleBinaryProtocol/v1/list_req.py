#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_list_message(path):
    version = 1
    cmd = 7
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)


def recv_ans(sck):
    # message version
    check_version(sck, 1)
    # message type
    check_type(sck, 7)
    # message category
    check_category(sck, 1)
    # message status
    status = read_int(sck, 2)
    if status == 0:
        pass
        # check padding
        check_padding(sck, 2)
        # get number of files
        N = read_int(sck, 4)
        # read all strings
        files = []
        for i in range(N):
            s = read_string(sck)
            files.append(s)
        print("Found", N, "files:")
        for i,f in zip(range(N), files):
            print(i,") ", f, sep='')
    else:
        check_padding(sck, 2)
        print("Bad response")

port = 5050
path = "." if len(sys.argv) == 1 else sys.argv[1]

if __name__ == "__main__":
    cmd = serialize_list_message(path)
    sck = send_cmd(port, cmd)
    sck.settimeout(100)
    #time.sleep(1)
    recv_ans(sck)