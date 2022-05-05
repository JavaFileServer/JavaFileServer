package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

public class SimpleBinarySchedulableCreateOrReplaceCommand extends SchedulableCreateOrReplaceCommand implements AutoCloseable {
    private static class Result {
        private Semaphore sem;
        public Result() {
            this.sem = new Semaphore(0);
        }

        private boolean status;
        public void update(boolean r) {
            this.status = r;
            // inc semaphore
            this.sem.release();
        }

        public boolean success() throws InterruptedException {
            this.sem.acquire();
            return status;
        }
    }

    private Identity user;
    @Override
    public void setUser(Identity user) {
        this.user = user;
    }

    @Override
    public Identity getUser() {
        return this.user;
    }

    private Result res;
    private BufferManager.BufferWrapper wrapper;
    private SimpleBinarySchedulableCreateOrReplaceCommand(Result res, Path path, BufferManager.BufferWrapper data, boolean sync)
    {
        super(path, data.get(), sync);
        this.res = res;
        this.wrapper = data;
    }

    private SimpleBinarySchedulableCreateOrReplaceCommand(Result res, Path path, BufferManager.BufferWrapper data) {
        this(res, path, data, false);
    }

    private boolean done;
    @Override
    public void reply(boolean success) throws Exception {
        if (!done) {
            done = true;
            this.res.update(success);
            if (wrapper != null) {
                wrapper.close();
            }
        } else {
            throw new Exception("Cannot reply twice!");
        }
    }

    private static void reply(SocketChannel sc, int version, boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(version);   // version
        bs.writeShort(2);  // command: CREATE OR REPLACE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        sc.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    public static void handle(Executor executor, SocketChannel sc, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(sc);
        var path = new Path(SimpleBinaryHelper.readString(sc));
        var length = version < 4 ? SimpleBinaryHelper.readInt(sc) : SimpleBinaryHelper.readLong(sc);
        var result = new Result();

        // get a buffer
        var wrapper = BufferManager.getBuffer();
        var buffer = wrapper.get();
        // how many bytes to read now?
        var toRead = (int)Math.min(length, buffer.remaining());
        buffer.limit(toRead);
        do {
            if (sc.read(buffer) < 0) {
                throw new Exception("Bad read");
            }
        } while (buffer.hasRemaining());
        // ready to read
        buffer.flip();
        var offset = buffer.remaining();
        // prepare command
        var schedulable = new SimpleBinarySchedulableCreateOrReplaceCommand(result, path, wrapper);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
        // done?
        if (!result.success()) {
            reply(sc, version, false);
        } else if (length != offset) {
            var success = SimpleBinarySchedulableWriteCommand.write(executor, sc, user, path, offset, length-offset);
            reply(sc, version, success);
        } else {
            reply(sc, version, true);
        }
    }

    @Override
    public void close() throws Exception {
        if (wrapper != null) {
            wrapper.close();
        }
    }
}