package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.CreateOrReplaceCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.filemanagers.FileManager;

public abstract class SchedulableCreateOrReplaceCommand extends CreateOrReplaceCommand implements SchedulableCommand {
    protected SchedulableCreateOrReplaceCommand(CreateOrReplaceCommand cmd)
    {
        super(cmd);
    }

    protected SchedulableCreateOrReplaceCommand(Path path, ByteBuffer data, boolean sync)
    {
        super(path, data, sync);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(FileManager exe) throws ApplicationException {
        exe.handle(this);
    }
}
