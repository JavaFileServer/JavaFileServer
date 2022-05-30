package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.SizeCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.filemanagers.FileManager;

public abstract class SchedulableSizeCommand extends SizeCommand implements SchedulableCommand {
    protected SchedulableSizeCommand(SizeCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(long size) throws Exception;
    public abstract void notFound() throws Exception;

    @Override
    public final void submit(FileManager exe) throws ApplicationException {
        exe.handle(this);
    }
}
