package it.sssupserver.app.exceptions;

/**
 * Base class for are exceptions signaling
 * application errors.
 */
public class ApplicationException extends Exception {
    public ApplicationException(String msg) {
        super(msg);
    }

    public ApplicationException(Throwable e) {
        super(e);
    }

    public ApplicationException(String msg, Throwable e) {
        super(msg, e);
    }
}
