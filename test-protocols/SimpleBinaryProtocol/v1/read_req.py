#!/usr/bin/python3

import socket
import time
from utils import *
import sys


def serialize_write_message(path, begin = 0, length = 0):
    version = 1
    cmd = 1
    category = 0

    return version.to_bytes(4,byteorder='big')+\
        cmd.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        begin.to_bytes(4,byteorder='big')+\
        length.to_bytes(4,byteorder='big')


def recv_ans(sck):
    # message version
    check_version(sck, 1)
    # message type
    check_type(sck, 1)
    # message category
    check_category(sck, 1)
    # message status
    status = read_int(sck, 2)
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

def usage(comm):
    print("Usage:")
    print("\t", comm, "path", "[offset]", "[length]")
    exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        usage(sys.argv[0])

    path = sys.argv[1]
    offset = 0 if len(sys.argv) <= 2 else int(sys.argv[2])
    length = 0 if len(sys.argv) <= 3 else int(sys.argv[3])

    print("Test READ request")
    #s = "Ciao panino al caffééé"
    #b = serialize_string(s)
    #print(s, '=>', b)
    #print('decoded: ', deserialize_string(b))
    cmd = serialize_write_message(path, offset, length)
    sck = send_cmd(port, cmd)
    sck.settimeout(1)
    #time.sleep(1)
    recv_ans(sck)
    sck.close()