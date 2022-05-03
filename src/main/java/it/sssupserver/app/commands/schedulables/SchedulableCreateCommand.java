package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.CreateCommand;

public abstract class SchedulableCreateCommand extends CreateCommand implements SchedulableCommand {
    protected SchedulableCreateCommand(CreateCommand cmd)
    {
        super(cmd);
    }

    protected SchedulableCreateCommand(Path path, ByteBuffer data, boolean sync)
    {
        super(path, data, sync);
    }

    public abstract void reply(boolean success) throws Exception;
}
