package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.DeleteCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.filemanagers.FileManager;

public abstract class SchedulableDeleteCommand extends DeleteCommand implements SchedulableCommand {
    protected SchedulableDeleteCommand(DeleteCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(FileManager exe) throws ApplicationException {
        exe.handle(this);
    }
}
