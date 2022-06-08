package it.sssupserver.app.exceptions;

import it.sssupserver.app.commands.Type;

/**
 * Exception thrown by an executor not supporting
 * a given command.
 */
public class CommandNotSupportedException extends ApplicationException {
    private Type type;
    public CommandNotSupportedException(Type type) {
        super(type.toString());
    }

    public CommandNotSupportedException(Type type, String msg) {
        super(msg + type.toString());
    }

    public Type getType() {
        return type;
    }
}
