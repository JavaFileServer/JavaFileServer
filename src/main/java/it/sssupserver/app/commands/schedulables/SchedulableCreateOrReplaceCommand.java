package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.CreateOrReplaceCommand;

public abstract class SchedulableCreateOrReplaceCommand extends CreateOrReplaceCommand implements SchedulableCommand {
    protected SchedulableCreateOrReplaceCommand(CreateOrReplaceCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
