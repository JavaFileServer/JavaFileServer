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
        serialize_string(content)

def recv_ans(sck):
    print("Receving response from socket")
    # message version
    v = sck.recv(4, socket.MSG_WAITALL)
    if len(v) != 4:
        raise Exception("Bad read "+ str(len(v)))
    version = int.from_bytes(v, byteorder='big')
    if version != 1:
        raise Exception("Bad version, found:", version)
    # message type
    t = sck.recv(2, socket.MSG_WAITALL)
    if len(t) != 2:
        raise Exception("Bad read")
    typem = int.from_bytes(t, byteorder='big')
    if typem != 5:
        raise Exception("Bad message type, found:", typem)
    # message category
    cat = sck.recv(2, socket.MSG_WAITALL)
    if len(cat) != 2:
        raise Exception("Bad read")
    category = int.from_bytes(cat, byteorder='big')
    if category != 1:
        raise Exception("Bad category, found:", category)
    # message status
    s = sck.recv(1, socket.MSG_WAITALL)
    if len(s) != 1:
        raise Exception("Bad read")
    status = int.from_bytes(s, byteorder='big')
    if status != 1:
        raise Exception("Bad status, found:", status)
    # check padding
    p = sck.recv(3, socket.MSG_WAITALL)
    if len(p) != 3:
        raise Exception("Bad read")
    padding = int.from_bytes(p, byteorder='big')
    if padding != 0:
        raise Exception("Bad padding, found:", padding)
    print("File successfully updated!")

port = 5050
path = "file/base" if len(sys.argv) < 2 else sys.argv[2]
content = "[! new message piece!]" if len(sys.argv) == 1 else sys.argv[1]

if __name__ == "__main__":
    print("Test CREATE OR REPLACE request")
    print("Message that will be uploaded!")
    cmd = serialize_append_message(path, content)
    print("Write MSG =>", cmd)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
