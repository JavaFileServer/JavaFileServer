package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import it.sssupserver.app.commands.ReadCommand;

public abstract class SchedulableReadCommand extends ReadCommand implements SchedulableCommand {
    protected SchedulableReadCommand(ReadCommand cmd)
    {
        super(cmd);
    }

    /**
     * Send a chunk (but not the last) of the response.
     */
    public void partial(FileChannel fc, long length) throws Exception {
        // number of bytes to read
        var toRead = length;
        // number of buffer to use
        var nBuffers = (toRead + Integer.MAX_VALUE-1) / Integer.MAX_VALUE;
        if (nBuffers > Integer.MAX_VALUE) {
            throw new Exception("Too much data required");
        }
        var buffers = new ByteBuffer[(int)nBuffers];
        for (int i = 1; i != nBuffers; ++i) {
            buffers[i-1] = ByteBuffer.allocateDirect(Integer.MAX_VALUE);
        }
        buffers[(int)nBuffers-1] = ByteBuffer.allocateDirect((int)(toRead % Integer.MAX_VALUE));
        fc.write(buffers);
        this.partial(buffers);
    }
    public void partial(ByteBuffer data) throws Exception {
        this.partial(new ByteBuffer[]{data});
    }
    public abstract void partial(ByteBuffer[] data) throws Exception;

    /**
     * Send the (last chunk) of the response
     */
    public void reply(FileChannel fc, long length) throws Exception {
        // number of bytes to read
        var toRead = length;
        // number of buffer to use
        var nBuffers = (toRead + Integer.MAX_VALUE-1) / Integer.MAX_VALUE;
        if (nBuffers > Integer.MAX_VALUE) {
            throw new Exception("Too much data required");
        }
        var buffers = new ByteBuffer[(int)nBuffers];
        for (int i = 1; i != nBuffers; ++i) {
            buffers[i-1] = ByteBuffer.allocateDirect(Integer.MAX_VALUE);
        }
        buffers[(int)nBuffers-1] = ByteBuffer.allocateDirect((int)(toRead % Integer.MAX_VALUE));
        fc.write(buffers);
        this.reply(buffers);
    }
    public void reply(ByteBuffer data) throws Exception {
        this.reply(new ByteBuffer[]{data});
    }
    public abstract void reply(ByteBuffer[] data) throws Exception;
    public abstract void notFound() throws Exception;
}
