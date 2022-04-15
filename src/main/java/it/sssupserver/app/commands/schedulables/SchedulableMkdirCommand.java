package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.MkdirCommand;

public abstract class SchedulableMkdirCommand extends MkdirCommand implements SchedulableCommand {
    protected SchedulableMkdirCommand(MkdirCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
