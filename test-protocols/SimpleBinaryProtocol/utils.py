#!/usr/bin/python3

import socket

def check_version(sck, version):
    v = sck.recv(4, socket.MSG_WAITALL)
    if len(v) != 4:
        raise Exception("Bad read "+ str(len(v)))
    found = int.from_bytes(v, byteorder='big')
    if version != found:
        raise Exception("Bad version, found:", found, "instead of", version)

def check_category(sck, category):
    cat = sck.recv(2, socket.MSG_WAITALL)
    if len(cat) != 2:
        raise Exception("Bad read")
    found = int.from_bytes(cat, byteorder='big')
    if category != found:
        raise Exception("Bad category, found:", found, "instead of", category)

def check_padding(sck, lenght):
    p = sck.recv(lenght, socket.MSG_WAITALL)
    if len(p) != lenght:
        raise Exception("Bad read")
    padding = int.from_bytes(p, byteorder='big')
    if padding != 0:
        raise Exception("Bad flags, found:", padding)

def serialize_string(string):
    b = bytes(string, "utf-8")
    l = len(b)
    return l.to_bytes(4,byteorder='big')+b

def deserialize_string(byteseq):
    if len(byteseq) < 4:
        raise Exception("Non sense sequence")

    l = int.from_bytes(byteseq[:4], byteorder='big')
    if len(byteseq) < l+4:
        raise Exception("Sequence too short")

    s = byteseq[4:4+l].decode('utf-8')
    return (s, l+4)

def send_cmd(port, msg):
    print("Connecting TCP socket to port", port)
    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sck.connect(('localhost', port))
    print("Sending data")
    sck.send(msg)
    print("Data sent!")
    return sck
