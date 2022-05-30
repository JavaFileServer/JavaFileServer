package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.AppendCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.filemanagers.FileManager;

public abstract class SchedulableAppendCommand extends AppendCommand implements SchedulableCommand {
    protected SchedulableAppendCommand(AppendCommand cmd)
    {
        super(cmd);
    }

    public SchedulableAppendCommand(Path path, ByteBuffer data, boolean sync) {
        super(path, data, sync);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(FileManager exe) throws ApplicationException {
        exe.handle(this);
    }
}
