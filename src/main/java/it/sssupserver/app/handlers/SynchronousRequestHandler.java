package it.sssupserver.app.handlers;

import it.sssupserver.app.commands.Command;

public interface SynchronousRequestHandler extends RequestHandler {
    /**
     * Wait for a return a command to the caller.
     * Blocking and synchronous.
     * To be used mainly for testing purpose.
     * Does not require the use of start()/stop().
     */
    public Command receiveCommand() throws Exception;
    /**
     * Wait for a command and execute it.
     * Blocking and synchronous.
     * To be used mainly for testing purpose.
     * Does not require the use of start()/stop().
     */
    public void receiveAndExecuteCommand() throws Exception;
}
