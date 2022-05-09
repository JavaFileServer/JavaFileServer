package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableCopyCommand extends SchedulableCopyCommand {
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
    public SimpleBinarySchedulableCopyCommand(CopyCommand cmd, SocketChannel out, int version, int marker) {
        super(cmd);
        this.out = out;
        this.version = version;
        this.marker = marker;
    }

    @Override
    public void reply(boolean success) throws Exception {
        // 12 bytes header + 4 if v>=3 + total_len for payload
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);    // version
            if (this.version >= 3) {
                buffer.putInt(this.marker);
            }
            buffer.putShort((short)10); // command: COPY
            buffer.putShort((short)1);  // category: answer
            buffer.put((byte)(success?1:0)); // result
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
        var src = SimpleBinaryHelper.readString(sc);
        var dst = SimpleBinaryHelper.readString(sc);
        var cmd = new CopyCommand(new Path(src), new Path(dst));
        var schedulable = new SimpleBinarySchedulableCopyCommand(cmd, sc, version, marker);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}
