package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to truncate the content of a file
 */
public class DeleteCommand implements Command {
    private Path path;

    public DeleteCommand(DeleteCommand cmd)
    {
        this(cmd.getPath());
    }

    public DeleteCommand(Path path)
    {
        this.path = path;
    }

    public Type getType()
    {
        return Type.DELETE;
    }

    public Path getPath()
    {
        return this.path;
    }

    @Override
    public String toString()
    {
        return "["+getType()+"]"+getPath();
    }
}
