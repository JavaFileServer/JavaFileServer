package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

import java.io.*;
import java.nio.ByteBuffer;
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

    private static void reply(SocketChannel sc, int version, boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(version);   // version
        bs.writeShort(5);  // command: APPEND
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        sc.write(ByteBuffer.wrap(bytes.toByteArray()));
    }

    public static void handle(Executor executor, SocketChannel sc, DataInputStream din, int version, Identity user, int marker) throws Exception {
        SimpleBinaryHandler.checkCategory(din);
        var path = new Path(SimpleBinaryHandler.readString(din));
        var length = version < 4 ? din.readInt() : din.readLong();
        var read = 0;
        var result = new Result();
        var success = true;

        do {
            // get a buffer
            var wrapper = BufferManager.getBuffer();
            var buffer = wrapper.get();
            // how many bytes to read now?
            var remainder = length - read;
            var toRead = (int)Math.min(remainder, buffer.remaining());
            // read bytes
            var buf = new byte[toRead];
            var tmp = din.read(buf);
            // success?
            if (tmp < 0) {
                throw new Exception("Bad read");
            }
            buffer.put(buf, 0, tmp);
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
        reply(sc, version, success);
    }

    @Override
    public void close() throws Exception {
        if (wrapper != null) {
            wrapper.close();
        }
    }
}
