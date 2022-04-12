package it.sssupserver.app.handlers;

import it.sssupserver.app.commands.Command;

/**
 * Interface implemented by all request handlers
 */
public interface RequestHandler {
    /**
     * Start the request handler
     * in a separate thread.
     * Non-blocking.
     */
    public void start() throws Exception;
    /**
     * Stop the request handler
     * operating in a separate context.
     * Cannot be used if start() has not
     * been called.
     * Blocking.
     */
    public void stop() throws Exception;
    /**
     * Wait for a return a command to the caller.
     * Blocking and synchronous.
     * To be used mainly for testing purpose.
     * Does not require the use of start()/stop().
     */
    public Command receiveCommand() throws Exception;
}
