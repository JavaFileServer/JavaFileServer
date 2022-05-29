package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.MkdirCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;

public abstract class SchedulableMkdirCommand extends MkdirCommand implements SchedulableCommand {
    protected SchedulableMkdirCommand(MkdirCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;

    @Override
    public final void submit(Executor exe) throws ApplicationException {
        exe.handle(this);
    }
}
