#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_delete_message(path):
    version = 1
    cmd = 6
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)


def recv_ans(sck):
    print("Receving response from socket")
    # message version
    check_version(sck, 1)
    # message type
    check_type(sck, 6)
    # message category
    check_category(sck, 1)
    # message status
    s = sck.recv(1, socket.MSG_WAITALL)
    if len(s) != 1:
        raise Exception("Bad read")
    success = int.from_bytes(s, byteorder='big')
    # check padding
    check_padding(sck, 3)
    print("File deleted: ", success == 1)

def usage(comm):
    print("Usage:")
    print("\t", comm, "path")
    exit(1)

port = 5050

if __name__ == "__main__":
    if len(sys.argv) < 2:
        usage(sys.argv[0])

    path = sys.argv[1]

    print("Test DELETE request")
    cmd = serialize_delete_message(path)
    print("Delete MSG =>", cmd)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)