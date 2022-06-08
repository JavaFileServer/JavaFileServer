#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_move_message(source, destination):
    version = 1
    cmd = 11
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
    check_type(sck, 11)
    # message category
    check_category(sck, 1)
    # message status
    status = read_int(sck, 1)
    # check padding
    check_padding(sck, 3)
    print("Status =", status == 1)

port = 5050

def usage(comm):
    print("Usage:", file=sys.stderr)
    print("\t", comm, "src-path", "dst-path", file=sys.stderr)
    exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        usage(sys.argv[0])

    src = sys.argv[1]
    dst = sys.argv[2]
    cmd = serialize_move_message(src, dst)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
    sck.close()