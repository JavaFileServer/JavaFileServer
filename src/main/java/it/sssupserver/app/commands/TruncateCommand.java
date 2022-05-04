package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to truncate the content of a file
 */
public class TruncateCommand implements Command {
    private Path path;
    private long length;

    public TruncateCommand(TruncateCommand cmd)
    {
        this(cmd.getPath(), cmd.getLength());
    }

    public TruncateCommand(Path path, long length)
    {
        this.path = path;
        this.length = length;
    }

    public TruncateCommand(Path path)
    {
        this(path, 0L);
    }

    public Type getType()
    {
        return Type.TRUNCATE;
    }

    public Path getPath()
    {
        return this.path;
    }

    public long getLength()
    {
        return this.length;
    }

    @Override
    public String toString()
    {
        return "["+getType()+"]"+getPath();
    }
}
