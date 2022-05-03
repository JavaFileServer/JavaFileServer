package it.sssupclient.app.exceptions;

/**
 * Exception thown by a command handler recognizing
 * a malformed input
 */
public class InvalidArgumentsException extends Exception {
    public InvalidArgumentsException() {
        super();
    }

    public InvalidArgumentsException(String msg) {
        super(msg);
    }
}
