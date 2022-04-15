package it.sssupserver.app.base;

/**
 * This class represents the base class of all
 * exceptions throwed by Executors while handling
 * commands
 */
public class ExecutionException extends Exception {
    public ExecutionException() {
        super();
    }

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(Throwable cause) {
        super(cause);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
