package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.WriteCommand;

public abstract class SchedulableWriteCommand extends WriteCommand implements SchedulableCommand {
    protected SchedulableWriteCommand(WriteCommand cmd)
    {
        super(cmd);
    }

    protected SchedulableWriteCommand(Path path, ByteBuffer data, int offset, boolean sync) {
        super(path, data, offset, sync);
    }

    public abstract void reply(boolean success) throws Exception;
}
