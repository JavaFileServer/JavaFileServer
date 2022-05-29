package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.ExistsCommand;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;

public abstract class SchedulableExistsCommand extends ExistsCommand implements SchedulableCommand {
    protected SchedulableExistsCommand(ExistsCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean exists) throws Exception;

    @Override
    public final void submit(Executor exe) throws ApplicationException {
        exe.handle(this);
    }
}
