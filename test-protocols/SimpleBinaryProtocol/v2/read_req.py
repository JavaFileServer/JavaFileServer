#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_write_message(username, path, begin = 0, length = 0):
    version = 2
    cmd = 1
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        serialize_string(username)+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        begin.to_bytes(4,byteorder='big')+\
        length.to_bytes(4,byteorder='big')


def recv_ans(sck):
    # message version
    check_version(sck, 2)
    # message type
    check_type(sck, 1)
    # message category
    check_category(sck, 1)
    # message status
    status = read_int(sck, 2)
    if status == 0:
        # message status
        flags = read_int(sck, 2)
        if (flags | 1) ^ 1 != 0:    # only bit 0 can be 1
            raise Exception("Bad flags, found:", flags)
        # data info
        begin = read_int(sck, 4)
        length = read_int(sck, 4)
        payload = read_all(sck, length)
        if len(payload) != length:
            raise Exception("Missing " + str(length -len(payload)) + " bytes")
        # other chunks availables
        if flags & 1:
            payload += recv_ans(sck)
        return payload
    elif status == 1:
        # check padding
        check_padding(sck, 2)
        print("Error while reading file", file=sys.stderr)
    else:
        raise Exception("Bad status, found:", status)

port = 5050

def usage(comm):
    print("Usage:", file=sys.stderr)
    print("\t", comm, "username", "path", "[offset]", "[length]", file=sys.stderr)
    exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        usage(sys.argv[0])

    username = sys.argv[1]
    path = sys.argv[2]
    offset = 0 if len(sys.argv) <= 3 else int(sys.argv[3])
    length = 0 if len(sys.argv) <= 4 else int(sys.argv[4])
    cmd = serialize_write_message(username, path, offset, length)
    sck = send_cmd(port, cmd)
    sck.settimeout(100)
    #time.sleep(1)
    payload = recv_ans(sck)
    sys.stdout.buffer.write(payload)
    sck.close()