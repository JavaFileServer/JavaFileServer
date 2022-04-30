#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_list_message(username, path):
    version = 2
    cmd = 7
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        serialize_string(username)+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)


def recv_ans(sck):
    # message version
    check_version(sck, 2)
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

def usage(comm):
    print("Usage:", file=sys.stderr)
    print("\t", comm, "username", "[path]", file=sys.stderr)
    exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        usage(sys.argv[0])

    username = sys.argv[1]
    path = "." if len(sys.argv) <= 2 else sys.argv[2]

    print("Test LIST request")
    cmd = serialize_list_message(username, path)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)