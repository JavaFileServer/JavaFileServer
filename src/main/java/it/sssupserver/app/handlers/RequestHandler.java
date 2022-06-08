package it.sssupserver.app.handlers;

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
}
