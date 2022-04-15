package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.CreateCommand;

public abstract class SchedulableCreateCommand extends CreateCommand implements SchedulableCommand {
    protected SchedulableCreateCommand(CreateCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
