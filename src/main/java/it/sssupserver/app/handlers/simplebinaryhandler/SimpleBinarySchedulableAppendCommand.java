package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

/**
 * An append is a special kind of operation that require serialization.
 * Chunks must be written one by one because underlying file size is
 * unknown. This makes this operation less efficient than general bulk
 * write because Executor(s) do not grant ordering in received operations
 * so
 */
public class SimpleBinarySchedulableAppendCommand extends SchedulableAppendCommand implements AutoCloseable {
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

    public SimpleBinarySchedulableAppendCommand(AppendCommand cmd, SocketChannel out) {
        super(cmd);
    }

    private Result res;
    private BufferManager.BufferWrapper wrapper;
    private SimpleBinarySchedulableAppendCommand(Result res, Path path, BufferManager.BufferWrapper data, boolean sync)
    {
        super(path, data.get(), sync);
        this.res = res;
        this.wrapper = data;
    }

    private SimpleBinarySchedulableAppendCommand(Result res, Path path, BufferManager.BufferWrapper data) {
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
        // 12 bytes header + 4 if v>=3 + total_len for payload
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(version);    // version
            if (version >= 3) {
                buffer.putInt(marker);
            }
            buffer.putShort((short)5);  // command: APPEND
            buffer.putShort((short)1);  // category: answer
            buffer.put((byte)(success?1:0)); // result
            buffer.put(new byte[3]);    // padding
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
        var read = 0L;
        var result = new Result();
        var success = true;

        do {
            // get a buffer
            var wrapper = BufferManager.getBuffer();
            var buffer = wrapper.get();
            // how many bytes to read now?
            var remainder = length - read;
            var toRead = (int)Math.min(remainder, buffer.remaining());
            buffer.limit(toRead);
            do {
                if (sc.read(buffer) < 0) {
                    throw new Exception("Bad read");
                }
            } while (buffer.hasRemaining());
            // ready to read
            buffer.flip();
            // increase counters
            read += buffer.remaining();
            // prepare command
            var schedulable = new SimpleBinarySchedulableAppendCommand(result, path, wrapper);
            schedulable.setUser(user);
            executor.scheduleExecution(schedulable);
            // done?
            if (!result.success()) {
                success = false;
                break;
            }
        } while (read != length);
        reply(sc, version, marker, success);
        // consume unused data
        if (read != length) {
            try (var wrapper = BufferManager.getBuffer();) {
                var buffer = wrapper.get();
                while (read != length) {
                    var remainder = length - read;
                    var toRead = (int)Math.min(remainder, buffer.remaining());
                    buffer.limit(toRead);
                    while (buffer.hasRemaining()) {
                        if (sc.read(buffer) <= 0) {
                            throw new Exception("Bad read");
                        }
                    }
                    buffer.clear();
                    length += toRead;
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (wrapper != null) {
            wrapper.close();
        }
    }
}
