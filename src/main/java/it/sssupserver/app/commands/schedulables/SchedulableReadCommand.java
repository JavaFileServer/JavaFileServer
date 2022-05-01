package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import it.sssupserver.app.commands.ReadCommand;

public abstract class SchedulableReadCommand extends ReadCommand implements SchedulableCommand {
    protected SchedulableReadCommand(ReadCommand cmd)
    {
        super(cmd);
    }

    public void reply(FileChannel fc, long offset, long length) throws Exception {
        var buffer = ByteBuffer.allocate((int)length);
        fc.write(buffer, offset);
        this.reply(buffer);
    }
    public void reply(ByteBuffer data) throws Exception {
        this.reply(new ByteBuffer[]{data});
    }
    public abstract void reply(ByteBuffer[] data) throws Exception;
    public abstract void notFound() throws Exception;
}
