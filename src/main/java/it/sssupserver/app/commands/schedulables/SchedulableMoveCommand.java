package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.MoveCommand;

public abstract class SchedulableMoveCommand extends MoveCommand implements SchedulableCommand {
    protected SchedulableMoveCommand(MoveCommand cmd)
    {
        super(cmd);
    }

    public abstract void reply(boolean success) throws Exception;
}
