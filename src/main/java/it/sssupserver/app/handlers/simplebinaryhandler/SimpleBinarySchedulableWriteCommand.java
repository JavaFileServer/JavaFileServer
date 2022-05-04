package it.sssupserver.app.handlers.simplebinaryhandler;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

public class SimpleBinarySchedulableWriteCommand extends SchedulableWriteCommand implements AutoCloseable {
    private static class Result {
        private Semaphore sem;
        public Result() {
            this.sem = new Semaphore(0);
        }

        private boolean status = true;
        public synchronized void update(boolean r) {
            // between locks
            this.status &= r;
            // inc semaphore
            this.sem.release();
        }

        public boolean success(int counter) throws Exception {
            this.sem.acquire(counter);
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

    private SimpleBinarySchedulableWriteCommand(WriteCommand cmd, SocketChannel out)
    {
        super(cmd);
    }

    private Result res;
    private BufferManager.BufferWrapper wrapper;
    private SimpleBinarySchedulableWriteCommand(Result res, Path path, BufferManager.BufferWrapper data, long offset, boolean sync)
    {
        super(path, data.get(), offset, sync);
        this.res = res;
        this.wrapper = data;
    }

    private SimpleBinarySchedulableWriteCommand(Result res, Path path, BufferManager.BufferWrapper data, long offset) {
        this(res, path, data, offset, false);
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
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(version);    // version
        if (version >= 3) {
            // write marker
            bs.writeInt(marker);
        }
        bs.writeShort(8);  // command: WRITE
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
        var offset = din.readInt();
        var length = din.readInt();
        var success = write(executor, din, user, path, offset, length);
        reply(sc, version, marker, success);
    }

    public static boolean write(Executor executor, DataInputStream din, Identity user, Path path, long offset, long length) throws IOException, Exception {
        var read = 0;
        var nChunks = 0;
        var res = new Result();

        while (read != length) {
            // one more chunk
            ++nChunks;
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
            // prepare command
            var schedulable = new SimpleBinarySchedulableWriteCommand(res, path, wrapper, offset);
            // increase counters
            offset += buffer.remaining();
            read += buffer.remaining();
            // send chunks one by one
            schedulable.setUser(user);
            executor.scheduleExecution(schedulable);
        }
        var success = res.success(nChunks);
        return success;
    }

    @Override
    public void close() throws Exception {
        if (wrapper != null) {
            wrapper.close();
        }
    }
}
