package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.CopyCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;

public abstract class SchedulableCopyCommand extends CopyCommand implements SchedulableCommand {
    protected SchedulableCopyCommand(CopyCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(Executor exe) throws ApplicationException {
        exe.handle(this);
    }
}
