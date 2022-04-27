package it.sssupserver.app.exceptions;

/**
 * Base class for are exceptions signaling
 * application errors.
 */
public class ApplicationException extends Exception {
    ApplicationException(String msg) {
        super(msg);
    }

    ApplicationException(Throwable e) {
        super(e);
    }

    ApplicationException(String msg, Throwable e) {
        super(msg, e);
    }
}
