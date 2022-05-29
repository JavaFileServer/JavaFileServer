package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.SizeCommand;
import it.sssupserver.app.commands.schedulables.SchedulableSizeCommand;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableSizeCommand extends SchedulableSizeCommand {
    private Identity user;
    @Override
    public void setUser(Identity user) {
        this.user = user;
    }

    @Override
    public Identity getUser() {
        return this.user;
    }

    private int version;
    private int marker;
    private SocketChannel out;
    public SimpleBinarySchedulableSizeCommand(SizeCommand cmd, SocketChannel out, int version, int marker) {
        super(cmd);
        this.out = out;
        this.version = version;
        this.marker = marker;
    }

    @Override
    public void reply(long size) throws Exception {
        // 16 bytes packet + 4 if v>=3 + 4 if v>=4
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);    // version
            if (this.version >= 3) {
                buffer.putInt(this.marker);
            }
            buffer.putShort((short)13); // command: SIZE
            buffer.putShort((short)1);  // category: answer
            buffer.putShort((short)0);  // status
            buffer.putShort((short)0);  // padding
            if (this.version < 4) {
                buffer.putInt((int)size);
            } else {
                buffer.putLong(size);
            }
            buffer.flip();
            // now data can be sent
            this.out.write(buffer);
        }
        // close connection
        this.out.close();
    }

    @Override
    public void notFound() throws Exception {
        // 16 bytes packet + 4 if v>=3 + 4 if v>=4
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);    // version
            if (this.version >= 3) {
                buffer.putInt(this.marker);
            }
            buffer.putShort((short)13); // command: SIZE
            buffer.putShort((short)1);  // category: answer
            buffer.putShort((short)1);  // error
            buffer.putShort((short)0);  // padding
            buffer.flip();
            // now data can be sent
            this.out.write(buffer);
        }
        // close connection
        this.out.close();
    }

    public static void handle(Executor executor, SocketChannel sc, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(sc);
        var path = SimpleBinaryHelper.readString(sc);
        var cmd = new SizeCommand(new Path(path));
        var schedulable = new SimpleBinarySchedulableSizeCommand(cmd, sc, version, marker);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}
