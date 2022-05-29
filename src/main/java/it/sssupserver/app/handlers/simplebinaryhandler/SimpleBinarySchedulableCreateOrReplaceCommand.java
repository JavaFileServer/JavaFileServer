package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

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

    private static void reply(SocketChannel sc, int version, int marker, boolean success) throws Exception {
        // 12 bytes header, no payload + 4 for marker in v>=3
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            // write data to buffer
            buffer.putInt(version);     // version
            if (version >= 3) {
                buffer.putInt(marker);
            }
            buffer.putShort((short)2);  // command: CREATE OR REPLACE
            buffer.putShort((short)1);  // category: answer
            buffer.put((byte)(success ? 1 : 0));  // data bytes
            buffer.put(new byte[3]);  // padding
            buffer.flip();
            // now data can be sent
            sc.write(buffer);
        }
        // close connection
        sc.close();
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
        SimpleBinaryHelper.readFull(sc, buffer);
        // ready to read
        long offset = toRead;
        // prepare command
        var schedulable = new SimpleBinarySchedulableCreateOrReplaceCommand(result, path, wrapper);
        schedulable.setUser(user);
        executor.scheduleExecution(schedulable);
        // done?
        if (!result.success()) {
            reply(sc, version, marker, false);
        } else if (length != offset) {
            var success = SimpleBinarySchedulableWriteCommand.write(executor, sc, user, path, offset, length-offset);
            reply(sc, version, marker, success);
        } else {
            reply(sc, version, marker, true);
        }
    }

    @Override
    public void close() throws Exception {
        if (wrapper != null) {
            wrapper.close();
        }
    }
}