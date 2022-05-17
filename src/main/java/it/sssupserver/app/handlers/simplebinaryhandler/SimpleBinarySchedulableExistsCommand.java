package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableExistsCommand extends SchedulableExistsCommand {
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
    public SimpleBinarySchedulableExistsCommand(ExistsCommand cmd, SocketChannel out, int version, int marker) {
        super(cmd);
        this.out = out;
        this.version = version;
        this.marker = marker;
    }

    @Override
    public void reply(boolean exists) throws Exception {
        // 12 bytes header + 4 if v>=3 + total_len for payload
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);    // version
            if (this.version >= 3) {
                buffer.putInt(this.marker);
            }
            buffer.putShort((short)3);  // command: EXISTS
            buffer.putShort((short)1);  // category: answer
            buffer.put((byte)(exists?1:0)); // result
            buffer.put(new byte[3]);    // padding
            buffer.flip();
            // now data can be sent
            this.out.write(buffer);
        }
        // close connection
        this.out.close();
    }

    public static void handle(Executor executor, SocketChannel sc, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(sc);
        String path = SimpleBinaryHelper.readString(sc);
        var cmd = new ExistsCommand(new Path(path));
        var schedulable = new SimpleBinarySchedulableExistsCommand(cmd, sc, version, marker);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }

    @Override
    public void submit(Executor exe) throws ApplicationException {
        exe.handle(this);
    }
}
