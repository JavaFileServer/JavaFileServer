package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to check if a file exists
 */
public class ExistsCommand implements Command {
    private Path path;

    public ExistsCommand(ExistsCommand cmd)
    {
        this(cmd.getPath());
    }

    public ExistsCommand(Path path)
    {
        this.path = path;
    }

    public Type getType()
    {
        return Type.EXISTS;
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
