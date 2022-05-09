package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.nio.channels.SocketChannel;
import java.util.Collection;

public class SimpleBinarySchedulableListCommand extends SchedulableListCommand {
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
    public SimpleBinarySchedulableListCommand(ListCommand cmd, SocketChannel out, int version, int marker) {
        super(cmd);
        this.out = out;
        this.version = version;
        this.marker = marker;
    }

    @Override
    public void reply(Collection<Path> content) throws Exception {
        var array = new byte[content.size()][];
        int i = 0;
        for (var item : content) {
            var tmp = SimpleBinaryHandler.serializeString(item.toString());
            array[i++] = tmp;
        }

        // 16 bytes header + 4 if v>=3 + total_len for payload
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);    // version
            if (this.version >= 3) {
                buffer.putInt(this.marker);
            }
            buffer.putShort((short)7);  // command: LIST
            buffer.putShort((short)1);  // category: answer
            buffer.putShort((short)0);  // status: OK
            buffer.putShort((short)0);  // padding
            buffer.putInt(array.length);    // number of strings
            for (var item : array) {
                buffer.put(item);
            }
            buffer.flip();
            // now data can be sent
            this.out.write(buffer);
        }
        // close connection
        this.out.close();
    }

    public void notFound() throws Exception {
        // 12 bytes header + 4 if v>=3, no payload
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);    // version
            if (this.version >= 3) {
                buffer.putInt(this.marker);
            }
            buffer.putShort((short)7);  // command: LIST
            buffer.putShort((short)1);  // category: answer
            buffer.putShort((short)1);  // status: ERROR
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
        var cmd = new ListCommand(new Path(path));
        var schedulable = new SimpleBinarySchedulableListCommand(cmd, sc, version, marker);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}
