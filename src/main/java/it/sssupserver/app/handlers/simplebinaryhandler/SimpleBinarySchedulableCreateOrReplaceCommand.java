package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableCreateOrReplaceCommand extends SchedulableCreateOrReplaceCommand {
    private Identity user;
    @Override
    public void setUser(Identity user) {
        this.user = user;
    }

    @Override
    public Identity getUser() {
        return this.user;
    }

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
        bs.writeInt(this.isAuthenticated() ? 2 : 1);    // version
        bs.writeShort(2);  // command: CREATE OR REPLACE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    public static void handle(Executor executor, SocketChannel sc, DataInputStream din, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(din);
        var path = SimpleBinaryHandler.readString(din);
        var data = SimpleBinaryHandler.readBytes(din);
        var cmd = new CreateOrReplaceCommand(new Path(path), ByteBuffer.wrap(data));
        var schedulable = new SimpleBinarySchedulableCreateOrReplaceCommand(cmd, sc);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}