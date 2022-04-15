package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.CopyCommand;

public abstract class SchedulableCopyCommand extends CopyCommand implements SchedulableCommand {
    protected SchedulableCopyCommand(CopyCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
