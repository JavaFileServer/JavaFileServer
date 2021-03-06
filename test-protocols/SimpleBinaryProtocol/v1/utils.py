#!/usr/bin/python3

import socket

def read_all(sck, length):
    buf = bytes()
    while length > 0:
        tmp = sck.recv(length, socket.MSG_WAITALL)
        buf += tmp
        length -= len(tmp)
    return buf

def read_int(sck, length):
    v = read_all(sck, length)
    if len(v) != length:
        raise Exception("Bad read "+ str(len(v)))
    return int.from_bytes(v, byteorder='big')

def check_version(sck, version):
    found = read_int(sck, 4)
    if version != found:
        raise Exception("Bad version, found:", found, "instead of", version)

def check_type(sck, cmd_type):
    found = read_int(sck, 2)
    if cmd_type != found:
        raise Exception("Bad type, found:", found, "instead of", cmd_type)

def check_category(sck, category):
    found = read_int(sck, 2)
    if category != found:
        raise Exception("Bad category, found:", found, "instead of", category)

def check_padding(sck, lenght):
    padding = read_int(sck, lenght)
    if padding != 0:
        raise Exception("Bad padding, found:", padding)

def serialize_bytes(data):
    l = len(data)
    return l.to_bytes(4,byteorder='big')+data

def serialize_string(string):
    b = bytes(string, "utf-8")
    return serialize_bytes(b)

def deserialize_string(byteseq):
    if len(byteseq) < 4:
        raise Exception("Non sense sequence")

    l = int.from_bytes(byteseq[:4], byteorder='big')
    if len(byteseq) < l+4:
        raise Exception("Sequence too short")

    s = byteseq[4:4+l].decode('utf-8')
    return (s, l+4)

def read_string(sck):
    L = read_int(sck, 4)
    B = sck.recv(L, socket.MSG_WAITALL)
    if len(B) != L:
        raise Exception("Bad read: get", len(B), "bytes instead of", L)
    S = deserialize_string(L.to_bytes(4,byteorder='big') + B)
    return S[0]

def send_cmd(port, msg):
    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sck.connect(('localhost', port))
    sck.send(msg)
    return sck

def check_int(sck, length, val):
    n = read_int(sck, length)
    if n != val:
        raise Exception("Malformed message, read", n, "instead of", val)
