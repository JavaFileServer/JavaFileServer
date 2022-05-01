package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
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

    private SocketChannel out;
    public SimpleBinarySchedulableListCommand(ListCommand cmd, SocketChannel out) {
        super(cmd);
        this.out = out;
    }

    @Override
    public void reply(Collection<Path> content) throws Exception {
        var array = new byte[content.size()][];
        int total_len = 0, i = 0;
        for (var item : content) {
            var tmp = SimpleBinaryHandler.serializeString(item.toString());
            total_len += tmp.length;
            array[i++] = tmp;
        }

        // 16 bytes header + total_len for payload
        var bytes = new ByteArrayOutputStream(16 + total_len);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.isAuthenticated() ? 2 : 1);    // version
        bs.writeShort(7);  // command: LIST
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(0);  // padding
        bs.writeInt(array.length);  // number of strings
        for (var item : array) {
            bs.write(item);
        }
        bs.flush();
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    public void notFound() throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.isAuthenticated() ? 2 : 1);    // version
        bs.writeShort(7);  // command: LIST
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: ERROR
        bs.writeShort(0);  // padding
        bs.flush();
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }
}
