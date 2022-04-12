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


def serialize_write_message(path, begin = 0, length = 0):
    version = 1
    read = 1
    category = 1

    return version.to_bytes(4,byteorder='big')+\
        read.to_bytes(2,byteorder='big')+\
        category.to_bytes(2,byteorder='big')+\
        serialize_string(path)+\
        begin.to_bytes(4,byteorder='big')+\
        length.to_bytes(4,byteorder='big')

def send_cmd(port, msg):
    print("Connecting TCP socket to port", port)
    sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sck.connect(('localhost', port))
    print("Sending data")
    sck.send(msg)
    print("Data sent!")
    sck.close()
    print("Socket closed")


port = 5050
path = "file/base"

if __name__ == "__main__":
    print("Test WRITE request")
    s = "Ciao panino al caffééé"
    b = serialize_string(s)
    print(s, '=>', b)
    print('decoded: ', deserialize_string(b))
    cmd = serialize_write_message(path)
    print("Write MSG =>", cmd)
    send_cmd(port, cmd)