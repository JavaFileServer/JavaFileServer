package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.ListCommand;

public abstract class SchedulableListCommand extends ListCommand implements SchedulableCommand {
    protected SchedulableListCommand(ListCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
