package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.MoveCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.filemanagers.FileManager;

public abstract class SchedulableMoveCommand extends MoveCommand implements SchedulableCommand {
    protected SchedulableMoveCommand(MoveCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(FileManager exe) throws ApplicationException {
        exe.handle(this);
    }
}
