package it.sssupserver.app.executors;

import it.sssupserver.app.commands.schedulables.SchedulableCommand;

public interface SynchronousExecutor extends Executor {
    /**
     * Execute a single command.
     * Blocking.
     */
    public void execute(SchedulableCommand command) throws Exception;
}
