package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
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
    private long offset;

    private int version;
    private int marker;
    private SimpleBinarySchedulableReadCommand(ReadCommand cmd, SocketChannel out, int version, int marker) {
        super(cmd);
        this.out = out;
        this.version = version;
        this.marker = marker;
    }

    @Override
    public void partial(ByteBuffer[] data) throws Exception {
        var length = 0;
        for (var a : data) {
            length += a.limit()-a.position();
        }
        // 20 bytes header + payload + 12 for v 4
        var bytes = new ByteArrayOutputStream(20+12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(version);    // version
        if (version >= 3) {
            // write marker
            bs.writeInt(this.marker);
        }
        bs.writeShort(1);  // command: READ
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(1);  // bitfield: end of answer
        if (this.version < 4) {
            bs.writeInt((int)offset);    // offset from the beginning
            bs.writeInt((int)length);    // data length
        } else {
            bs.writeLong(offset);
            bs.writeLong(length);
        }
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
        // 20 bytes header + payload + 12 for v 4
        var bytes = new ByteArrayOutputStream(20+12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(version);    // version
        if (version >= 3) {
            // write marker
            bs.writeInt(this.marker);
        }
        bs.writeShort(1);  // command: READ
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(0);  // bitfield: end of answer
        if (this.version < 4) {
            bs.writeInt((int)offset);    // offset from the beginning
            bs.writeInt((int)length);    // data length
        } else {
            bs.writeLong(offset);
            bs.writeLong(length);
        }
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
        // 12 bytes packet + 4 for v 3
        var bytes = new ByteArrayOutputStream(20+4);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(this.version);    // version
        if (this.version >= 3) {
            // write marker
            bs.writeInt(this.marker);
        }
        bs.writeShort(1);  // command: WRITE
        bs.writeShort(1);  // category: answer
        bs.writeShort(1);  // status: ERRORE
        bs.writeShort(0);  // padding
        // now data can be sent
        this.out.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    public static void handle(Executor executor, SocketChannel sc, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(sc);
        String path = SimpleBinaryHelper.readString(sc);
        long begin = version < 4 ? SimpleBinaryHelper.readInt(sc) : SimpleBinaryHelper.readLong(sc);
        long len   = version < 4 ? SimpleBinaryHelper.readInt(sc) : SimpleBinaryHelper.readLong(sc);
        var cmd = new ReadCommand(new Path(path), begin, len);
        var schedulable = new SimpleBinarySchedulableReadCommand(cmd, sc, version, marker);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
    }
}

