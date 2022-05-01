package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableReadCommand extends SchedulableReadCommand {
    private SocketChannel out;
    public SimpleBinarySchedulableReadCommand(ReadCommand cmd, SocketChannel out) {
        super(cmd);
        this.out = out;
    }

    @Override
    public void reply(byte[] data) throws Exception {
        // 20 bytes header + payload
        var bytes = new ByteArrayOutputStream(20+data.length);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(1);  // command: READ
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(0);  // bitfield: end of answer
        bs.writeInt(0);    // offset from the beginning
        bs.writeInt(data.length);    // data length
        bs.write(data);    // data bytes
        bs.flush();
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    @Override
    public void notFound() throws Exception {
        // 12 bytes packet
        var bytes = new ByteArrayOutputStream(20);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(1);  // command: WRITE
        bs.writeShort(1);  // category: answer
        bs.writeShort(1);  // status: ERRORE
        bs.writeShort(0);  // padding
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }
}

