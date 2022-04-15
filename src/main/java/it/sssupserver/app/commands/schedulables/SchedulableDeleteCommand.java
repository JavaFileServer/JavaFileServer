package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.DeleteCommand;

public abstract class SchedulableDeleteCommand extends DeleteCommand implements SchedulableCommand {
    protected SchedulableDeleteCommand(DeleteCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
