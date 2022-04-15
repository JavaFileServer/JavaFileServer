package it.sssupserver.app.commands.schedulables;

import java.util.Collection;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.ListCommand;

public abstract class SchedulableListCommand extends ListCommand implements SchedulableCommand {
    protected SchedulableListCommand(ListCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(Collection<Path> content) throws Exception;
    public abstract void notFound() throws Exception;
}
