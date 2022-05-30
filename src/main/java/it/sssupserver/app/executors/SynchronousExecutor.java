package it.sssupserver.app.filemanagers;

import it.sssupserver.app.commands.schedulables.SchedulableCommand;

public interface SynchronousExecutor extends FileManager {
    /**
     * Execute a single command.
     * Blocking.
     */
    public void execute(SchedulableCommand command) throws Exception;
}
