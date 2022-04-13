package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.AppendCommand;

public abstract class SchedulableAppendCommand extends AppendCommand implements SchedulableCommand {
    protected SchedulableAppendCommand(AppendCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
