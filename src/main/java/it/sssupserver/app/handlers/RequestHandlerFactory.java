package it.sssupserver.app.handlers;

/**
 * This class will be used to supply request
 * hadlers based on the supplied parameters
 */
public class RequestHandlerFactory {
    public static RequestHandler getRequestHandler() throws Exception
    {
        return new SimpleBinaryHandler();
    }
}
