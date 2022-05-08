package it.sssupserver.app.handlers;

import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.handlers.simplebinaryhandler.*;

/**
 * This class will be used to supply request
 * hadlers based on the supplied parameters
 */
public class RequestHandlerFactory {
    // Command line arguments starting with this prefix
    // are intended as directed to a request handler
    private static final String argsPrefix = "--H";

    public static RequestHandler getRequestHandler(Executor executor) throws Exception
    {
        return getRequestHandler(executor, null);
    }

    public static RequestHandler getRequestHandler(Executor executor, String[] args) throws Exception
    {
        return new SimpleBinaryHandler(executor);
    }
}
