#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_mkdir_message(username, path):
    version = 2
    cmd = 12
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
    check_type(sck, 12)
    # message category
    check_category(sck, 1)
    # message status
    s = sck.recv(1, socket.MSG_WAITALL)
    if len(s) != 1:
        raise Exception("Bad read")
    success = int.from_bytes(s, byteorder='big')
    # check padding
    check_padding(sck, 3)
    print("Directory created: ", success == 1)

port = 5050

def usage(comm):
    print("Usage:", file=sys.stderr)
    print("\t", comm, "username", "path", file=sys.stderr)
    exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        usage(sys.argv[0])

    username = sys.argv[1]
    path = sys.argv[2]

    print("Test MKDIR request")
    cmd = serialize_mkdir_message(username, path)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)