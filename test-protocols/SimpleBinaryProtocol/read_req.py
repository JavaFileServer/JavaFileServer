#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_write_message(path, begin = 0, length = 0):
    version = 1
    read = 1
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        read.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        begin.to_bytes(4,byteorder='big')+\
        length.to_bytes(4,byteorder='big')


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
    if typem != 1:
        raise Exception("Bad message type, found:", typem)
    # message category
    cat = sck.recv(2, socket.MSG_WAITALL)
    if len(cat) != 2:
        raise Exception("Bad read")
    category = int.from_bytes(cat, byteorder='big')
    if category != 1:
        raise Exception("Bad category, found:", category)
    # message status
    s = sck.recv(2, socket.MSG_WAITALL)
    if len(s) != 2:
        raise Exception("Bad read")
    status = int.from_bytes(s, byteorder='big')
    if status == 0:
        # message status
        f = sck.recv(2, socket.MSG_WAITALL)
        if len(f) != 2:
            raise Exception("Bad read")
        flags = int.from_bytes(f, byteorder='big')
        if flags != 0:
            raise Exception("Bad flags, found:", flags)
        # data info
        d = sck.recv(8, socket.MSG_WAITALL)
        if len(d) != 8:
            raise Exception("Bad read")
        begin = int.from_bytes(d[:4], byteorder='big')
        length = int.from_bytes(d[4:], byteorder='big')
        print("Data: (",begin,':',begin+length,')', sep='')
        payload = sck.recv(length, socket.MSG_WAITALL)
        if len(payload) != length:
            raise Exception("Missing " + str(length -len(payload)) + " bytes")
        text = payload.decode('utf-8')
        print("Payload: ", "'"+text+"'")

    elif status == 1:
        # check padding
        check_padding(sck, 2)
#        p = sck.recv(2, socket.MSG_WAITALL)
#        if len(p) != 2:
#            raise Exception("Bad read")
#        padding = int.from_bytes(p, byteorder='big')
#        if padding != 0:
#            raise Exception("Bad flags, found:", padding)

        print("Error while reading file")
    else:
        raise Exception("Bad status, found:", status)

port = 5050
path = "file/base" if len(sys.argv) == 1 else sys.argv[1]

if __name__ == "__main__":
    print("Test WRITE request")
    s = "Ciao panino al caffééé"
    b = serialize_string(s)
    print(s, '=>', b)
    print('decoded: ', deserialize_string(b))
    cmd = serialize_write_message(path)
    print("Write MSG =>", cmd)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
    sck.close()