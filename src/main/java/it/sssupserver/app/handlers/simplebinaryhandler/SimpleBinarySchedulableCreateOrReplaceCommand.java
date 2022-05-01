package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableCreateOrReplaceCommand extends SchedulableCreateOrReplaceCommand {
    private SocketChannel out;
    public SimpleBinarySchedulableCreateOrReplaceCommand(CreateOrReplaceCommand cmd, SocketChannel out) {
        super(cmd);
        this.out = out;
    }

    @Override
    public void reply(boolean success) throws Exception {
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
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }
}