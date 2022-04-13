package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.ExistsCommand;

public abstract class SchedulableExistsCommand extends ExistsCommand implements SchedulableCommand {
    protected SchedulableExistsCommand(ExistsCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean exists) throws Exception;
}
