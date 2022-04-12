#!/usr/bin/python3

import socket

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
