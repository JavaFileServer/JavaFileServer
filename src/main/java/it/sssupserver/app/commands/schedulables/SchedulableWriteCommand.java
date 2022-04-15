package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.WriteCommand;

public abstract class SchedulableWriteCommand extends WriteCommand implements SchedulableCommand {
    protected SchedulableWriteCommand(WriteCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
