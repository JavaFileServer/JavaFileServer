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
        var length = data.limit()-data.position();
        var bytes = new byte[length];
        data.get(bytes);
        this.reply(data);
    }
    public abstract void reply(byte[] data) throws Exception;
    public abstract void notFound() throws Exception;
}
