package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.ReadCommand;

public abstract class SchedulableReadCommand extends ReadCommand implements SchedulableCommand {
    protected SchedulableReadCommand(ReadCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(byte[] data) throws Exception;
    public abstract void notFound() throws Exception;
}
