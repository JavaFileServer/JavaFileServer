package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

public class SimpleBinarySchedulableReadCommand extends SchedulableReadCommand {
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
    // remember the offset of the first elementh of the current chunk in
    // the stream of chunks sent back to the client
    private int offset;

    public SimpleBinarySchedulableReadCommand(ReadCommand cmd, SocketChannel out) {
        super(cmd);
        this.out = out;
    }

    @Override
    public void partial(ByteBuffer[] data) throws Exception {
        var length = 0;
        for (var a : data) {
            length += a.limit()-a.position();
        }
        // 20 bytes header + payload
        var bytes = new ByteArrayOutputStream(20);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.isAuthenticated() ? 2 : 1);    // version
        bs.writeShort(1);  // command: READ
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(1);  // bitfield: end of answer
        bs.writeInt(offset);    // offset from the beginning
        bs.writeInt(length);    // data length
        bs.flush();
        // scattered IO
        var ans = new ByteBuffer[data.length+1];
        // header
        ans[0] = ByteBuffer.wrap(bytes.toByteArray());
        // body
        for (int i = 0; i < data.length; ++i) {
            ans[1+i] = data[i];
        }
        // now data can be sent
        this.out.write(ans);

        // Partial response sent.
        offset += length;
    }

    @Override
    public void reply(ByteBuffer[] data) throws Exception {
        var length = 0;
        for (var a : data) {
            length += a.limit()-a.position();
        }
        // 20 bytes header + payload
        var bytes = new ByteArrayOutputStream(20);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.isAuthenticated() ? 2 : 1);    // version
        bs.writeShort(1);  // command: READ
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(0);  // bitfield: end of answer
        bs.writeInt(offset);    // offset from the beginning
        bs.writeInt(length);    // data length
        bs.flush();
        // scattered IO
        var ans = new ByteBuffer[data.length+1];
        // header
        ans[0] = ByteBuffer.wrap(bytes.toByteArray());
        // body
        for (int i = 0; i < data.length; ++i) {
            ans[1+i] = data[i];
        }
        // now data can be sent
        this.out.write(ans);
    }

    @Override
    public void notFound() throws Exception {
        // 12 bytes packet
        var bytes = new ByteArrayOutputStream(20);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.isAuthenticated() ? 2 : 1);    // version
        bs.writeShort(1);  // command: WRITE
        bs.writeShort(1);  // category: answer
        bs.writeShort(1);  // status: ERRORE
        bs.writeShort(0);  // padding
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    public static void handle(Executor executor, SocketChannel sc, DataInputStream din, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(din);
        String path = SimpleBinaryHandler.readString(din);
        int begin = din.readInt();
        int len = din.readInt();
        var cmd = new ReadCommand(new Path(path), begin, len);
        var schedulable = new SimpleBinarySchedulableReadCommand(cmd, sc);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}

