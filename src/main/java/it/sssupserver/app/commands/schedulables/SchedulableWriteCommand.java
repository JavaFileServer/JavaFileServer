package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.WriteCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.filemanagers.FileManager;

public abstract class SchedulableWriteCommand extends WriteCommand implements SchedulableCommand {
    protected SchedulableWriteCommand(WriteCommand cmd)
    {
        super(cmd);
    }

    protected SchedulableWriteCommand(Path path, ByteBuffer data, long offset, boolean sync) {
        super(path, data, offset, sync);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(FileManager exe) throws ApplicationException {
        exe.handle(this);
    }
}
