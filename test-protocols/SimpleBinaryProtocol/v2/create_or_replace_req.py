#!/usr/bin/python3

import socket
import time
from utils import *
import sys

def serialize_create_or_replace_message(username, path, content):
    version = 2
    cmd = 2
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        serialize_string(username)+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        serialize_string(content)

def recv_ans(sck):
    print("Receving response from socket")
    # message version
    check_version(sck, 2)
    # message type
    check_type(sck, 2)
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
    check_padding(sck, 0)
    print("File successfully updated!")

def usage(comm):
    print("Usage:")
    print("\t", comm, "username", "path", "data")
    exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 4:
        usage(sys.argv[0])

    port = 5050

    username = sys.argv[1]
    path = sys.argv[2]
    content = sys.argv[3]
    #path = "file/base" if len(sys.argv) <= 2 else sys.argv[2]
    #content = "Another NEW super beautiful message!" if len(sys.argv) == 1 else sys.argv[1]

    print("Test CREATE OR REPLACE request")
    print("Message that will be uploaded!")
    cmd = serialize_create_or_replace_message(username, path, content)
    print("Write MSG =>", cmd)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
