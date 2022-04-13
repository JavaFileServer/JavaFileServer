package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.TruncateCommand;

public abstract class SchedulableTruncateCommand extends TruncateCommand implements SchedulableCommand {
    protected SchedulableTruncateCommand(TruncateCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
