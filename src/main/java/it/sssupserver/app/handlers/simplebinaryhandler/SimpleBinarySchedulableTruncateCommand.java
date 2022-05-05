package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableTruncateCommand extends SchedulableTruncateCommand {
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
    private int version;
    private int marker;
    private SimpleBinarySchedulableTruncateCommand(Path path, long length, int version, int marker, SocketChannel out) {
        super(path, length);
        this.version = version;
        this.marker = marker;
        this.out = out;
    }

    @Override
    public void reply(boolean success) throws Exception {
        // 12 bytes header, no payload + 4 for v 4
        var bytes = new ByteArrayOutputStream(12+4);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.version);    // version
        if (this.version >= 3) {
            // write marker
            bs.writeInt(this.marker);
        }
        bs.writeShort(4);  // command: TRUNCATE
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
        String path = SimpleBinaryHelper.readString(sc);
        long length = 0;
        if (version >= 4) {
            length = SimpleBinaryHelper.readLong(sc);
        }
        var schedulable = new SimpleBinarySchedulableTruncateCommand(new Path(path), length, version, marker, sc);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}
