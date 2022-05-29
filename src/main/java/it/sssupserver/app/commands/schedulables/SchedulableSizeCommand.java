package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.SizeCommand;

public abstract class SchedulableSizeCommand extends SizeCommand implements SchedulableCommand {
    protected SchedulableSizeCommand(SizeCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(long size) throws Exception;
    public abstract void notFound() throws Exception;
}
