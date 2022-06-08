package it.sssupserver.app.commands;

/**
 * Interface implemented by all
 * command objects recongised by
 * the file server.
 */
public interface Command {
    /**
     * Return command type.
     */
    public Type getType();
}
