#!/usr/bin/python3

import socket
import time
from utils import *
import sys

def serialize_append_message(path, content):
    version = 1
    cmd = 5
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        serialize_bytes(content)

def recv_ans(sck):
    # message version
    check_version(sck, 1)
    # message type
    check_type(sck, 5)
    # message category
    check_category(sck, 1)
    # message status
    s = sck.recv(1, socket.MSG_WAITALL)
    if len(s) != 1:
        raise Exception("Bad read")
    status = int.from_bytes(s, byteorder='big')
    if status != 1:
        raise Exception("Bad status, found:", status)
    # check padding
    check_padding(sck, 3)
    print("File successfully updated!")

port = 5050

def usage(comm):
    print("Usage:", file=sys.stderr)
    print("\t", comm, "path", "[data]", file=sys.stderr)
    print("\tdata: is read from STDIN if not provided", file=sys.stderr)
    exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        usage(sys.argv[0])

    path = sys.argv[1]
    content = bytes(sys.argv[2], "utf-8") if len(sys.argv) == 3 else sys.stdin.buffer.read()
    cmd = serialize_append_message(path, content)
    sck = send_cmd(port, cmd)
    sck.settimeout(1000)
    #time.sleep(1)
    recv_ans(sck)
