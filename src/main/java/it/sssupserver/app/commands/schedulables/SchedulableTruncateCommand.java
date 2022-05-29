package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.TruncateCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;

public abstract class SchedulableTruncateCommand extends TruncateCommand implements SchedulableCommand {
    protected SchedulableTruncateCommand(TruncateCommand cmd)
    {
        super(cmd);
    }

    protected SchedulableTruncateCommand(Path path, long length)
    {
        super(path, length);
    }

    protected SchedulableTruncateCommand(Path path)
    {
        super(path, 0);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(Executor exe) throws ApplicationException {
        exe.handle(this);
    }
}
