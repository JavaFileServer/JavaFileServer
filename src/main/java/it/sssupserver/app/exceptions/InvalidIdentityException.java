package it.sssupserver.app.exceptions;

public class InvalidIdentityException extends ApplicationException {
    public InvalidIdentityException(String msg) {
        super(msg);
    }

    public InvalidIdentityException(Throwable e) {
        super(e);
    }

    public InvalidIdentityException(String msg, Throwable e) {
        super(msg, e);
    }
}
