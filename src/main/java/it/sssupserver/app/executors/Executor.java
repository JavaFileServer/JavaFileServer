package it.sssupserver.app.executors;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.repliers.*;

public interface Executor {
    /**
     * Start the executor
     * in a separate thread.
     * Non-blocking.
     */
    public void start();
    /**
     * Stop the executor
     * operating in a separate context.
     * Cannot be used if start() has not
     * been called.
     * Blocking.
     */
    public void stop();
    /**
     * Execute a single command.
     * Blocking.
     */
    public void execute(Command command, Replier replier);
}
