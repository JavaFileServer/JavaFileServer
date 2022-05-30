package it.sssupserver.app.filemanagers;

import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.exceptions.CommandNotSupportedException;

public interface FileManager {
    /**
     * Start the executor
     * in a separate thread.
     * Non-blocking.
     */
    public void start() throws Exception;
    /**
     * Stop the executor
     * operating in a separate context.
     * Cannot be used if start() has not
     * been called.
     * Blocking.
     */
    public void stop() throws Exception;
    /**
     * Schedule command for execution.
     * May be asynchronous and non-blocking.
     */
    public void scheduleExecution(SchedulableCommand command) throws Exception;

    /**
     * To be used internally by SchedulableCommands to implement
     * visitors pattern.
     */
    public default void handle(SchedulableCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableReadCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableCreateOrReplaceCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableTruncateCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableExistsCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableDeleteCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableAppendCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableListCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableWriteCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableCreateCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableCopyCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableMoveCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableMkdirCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }

    public default void handle(SchedulableSizeCommand command) throws ApplicationException {
        throw new CommandNotSupportedException(command.getType());
    }
}
