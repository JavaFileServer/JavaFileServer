package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to truncate the content of a file
 */
public class CopyCommand implements Command {
    private Path from;
    private Path to;

    public CopyCommand(CopyCommand cmd)
    {
        this(cmd.getSource(), cmd.getDestination());
    }

    public CopyCommand(Path from, Path to)
    {
        this.from = from;
        this.to = to;
    }

    public Type getType()
    {
        return Type.COPY;
    }

    public Path getSource()
    {
        return this.from;
    }

    public Path getDestination()
    {
        return this.to;
    }

    @Override
    public String toString()
    {
        return "["+getType()+"]"+getSource()+"|>"+getDestination();
    }
}
