package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableMoveCommand extends SchedulableMoveCommand {
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
    public SimpleBinarySchedulableMoveCommand(MoveCommand cmd, SocketChannel out) {
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
        bs.writeShort(11); // command: MOVE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
        // close connection
        this.out.close();
    }

    public static void handle(Executor executor, SocketChannel sc, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(sc);
        var src = SimpleBinaryHelper.readString(sc);
        var dst = SimpleBinaryHelper.readString(sc);
        var cmd = new MoveCommand(new Path(src), new Path(dst));
        var schedulable = new SimpleBinarySchedulableMoveCommand(cmd, sc);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}
