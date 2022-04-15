package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to list the content of a directory
 */
public class ListCommand implements Command {
    private Path path;

    public ListCommand(ListCommand cmd)
    {
        this(cmd.getPath());
    }

    public ListCommand(Path path)
    {
        this.path = path;
    }

    public Type getType()
    {
        return Type.LIST;
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
