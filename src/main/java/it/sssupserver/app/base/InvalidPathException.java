package it.sssupserver.app.base;

public class InvalidPathException extends ExecutionException {
    public InvalidPathException() {
        super();
    }

    public InvalidPathException(String message) {
        super(message);
    }

    public InvalidPathException(Throwable cause) {
        super(cause);
    }

    public InvalidPathException(String message, Throwable cause) {
        super(message, cause);
    }
}
