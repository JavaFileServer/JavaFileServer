package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to truncate the content of a file
 */
public class TruncateCommand implements Command {
    private Path path;

    public TruncateCommand(Path path)
    {
        this.path = path;
    }

    public Type getType()
    {
        return Type.TRUNCATE;
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
