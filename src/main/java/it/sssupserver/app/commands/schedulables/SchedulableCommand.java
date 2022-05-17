package it.sssupserver.app.commands.schedulables;

import it.sssupserver.app.commands.Command;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

/**
 * Represents a command with also the methods
 * to reply to the clients
 *
 * Maybe a method to return error could be useful
 */
public interface SchedulableCommand extends Command {
    public void setUser(Identity user);

    public default Identity getUser() {
        return null;
    }

    public default boolean isAuthenticated() {
        return this.getUser() != null;
    }

    /**
     * To be used called by Executor(s) to implement
     * visitors pattern.
     */
    public void submit(Executor exe) throws ApplicationException;
}
