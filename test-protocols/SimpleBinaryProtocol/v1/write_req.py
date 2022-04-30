#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_write_message(path, data, offset = 0):
    version = 1
    cmd = 8
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        offset.to_bytes(4,byteorder='big')+\
        serialize_string(data)


def recv_ans(sck):
    # message version
    check_version(sck, 1)
    # message type
    check_type(sck, 8)
    # message category
    check_category(sck, 1)
    # message status
    success = read_int(sck, 1)
    # check padding
    check_padding(sck, 3)
    print("Result of write operation:", success == 1)

def usage(comm):
    print("Usage:", file=sys.stderr)
    print("\t", comm, "path", "data", "[offset]", file=sys.stderr)
    exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        usage(sys.argv[0])

    port = 5050
    path = sys.argv[1]
    data = sys.argv[2]
    offset = 0 if len(sys.argv) <= 3 else int(sys.argv[3])

    print("Test WRITE request")
    cmd = serialize_write_message(path, data, offset)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
    sck.close()