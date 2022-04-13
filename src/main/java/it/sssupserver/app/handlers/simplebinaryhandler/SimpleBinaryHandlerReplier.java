package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.repliers.Replier;

import java.io.*;

class SimpleBinaryHandlerReplier extends Thread implements Replier {
    private DataOutputStream out;
    public SimpleBinaryHandlerReplier(DataOutputStream out)
    {
        this.out = out;
    }

    @Override
    public void replyRead(byte[] data) throws Exception {
        // 20 bytes header + payload
        var bytes = new ByteArrayOutputStream(20+data.length);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(1);  // command: WRITE
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(0);  // bitfield: end of answer
        bs.writeInt(0);    // offset from the beginning
        bs.writeInt(data.length);    // data length
        bs.write(data);    // data bytes
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyCreateOrReplace(boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(2);  // command: CREATE OR REPLACE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyAppend(boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(5);  // command: APPEND
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyExists(boolean exists) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(3);  // command: EXISTS
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(exists);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyTruncate(boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(4);  // command: TRUNCATE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }
}
