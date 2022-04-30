#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_copy_message(source, destination):
    version = 1
    cmd = 10
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(source)+\
        serialize_string(destination)


def recv_ans(sck):
    # message version
    check_version(sck, 1)
    # message type
    check_type(sck, 10)
    # message category
    check_category(sck, 1)
    # message status
    status = read_int(sck, 1)
    # check padding
    check_padding(sck, 3)
    print("Status =", status == 1)

port = 5050

def usage(comm):
    print("Usage:")
    print("\t", comm, "src-path", "dst-path")
    exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        usage(sys.argv[0])

    src = sys.argv[1]
    dst = sys.argv[2]

    print("Test COPY request")
    cmd = serialize_copy_message(src, dst)

    print("Copy MSG =>", cmd)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
    sck.close()