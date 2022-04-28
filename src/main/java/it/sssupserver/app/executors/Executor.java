package it.sssupserver.app.executors;

import it.sssupserver.app.commands.schedulables.SchedulableCommand;

public interface Executor {
    /**
     * Start the executor
     * in a separate thread.
     * Non-blocking.
     */
    public void start() throws Exception;
    /**
     * Stop the executor
     * operating in a separate context.
     * Cannot be used if start() has not
     * been called.
     * Blocking.
     */
    public void stop() throws Exception;
    /**
     * Schedule command for execution.
     * May be asynchronous and non-blocking.
     */
    public void scheduleExecution(SchedulableCommand command) throws Exception;
}
