package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;

import java.io.*;

public class SimpleBinarySchedulableExistsCommand extends SchedulableExistsCommand {
    private DataOutputStream out;
    public SimpleBinarySchedulableExistsCommand(ExistsCommand cmd, DataOutputStream out) {
        super(cmd);
        this.out = out;
    }

    @Override
    public void reply(boolean exists) throws Exception {
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
}
