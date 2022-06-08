package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.filemanagers.FileManager;
import it.sssupserver.app.users.Identity;

import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableDeleteCommand extends SchedulableDeleteCommand {
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
    public SimpleBinarySchedulableDeleteCommand(DeleteCommand cmd, SocketChannel out, int version, int marker) {
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
            buffer.putShort((short)6);  // command: DELETE
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

    public static void handle(FileManager executor, SocketChannel sc, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(sc);
        var path = SimpleBinaryHelper.readString(sc);
        var cmd = new DeleteCommand(new Path(path));
        var schedulable = new SimpleBinarySchedulableDeleteCommand(cmd, sc, version, marker);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}
