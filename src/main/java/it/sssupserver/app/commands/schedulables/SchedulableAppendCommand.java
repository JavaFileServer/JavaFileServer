package it.sssupserver.app.commands.schedulables;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.AppendCommand;

public abstract class SchedulableAppendCommand extends AppendCommand implements SchedulableCommand {
    protected SchedulableAppendCommand(AppendCommand cmd)
    {
        super(cmd);
    }

    public SchedulableAppendCommand(Path path, ByteBuffer data, boolean sync) {
        super(path, data, sync);
    }

    public abstract void reply(boolean success) throws Exception;
}
