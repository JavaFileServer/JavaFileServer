package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

/**
 * Simple command to truncate the content of a file
 */
public class MoveCommand implements Command {
    private Path from;
    private Path to;

    public MoveCommand(MoveCommand cmd)
    {
        this(cmd.getSource(), cmd.getDestination());
    }

    public MoveCommand(Path from, Path to)
    {
        this.from = from;
        this.to = to;
    }

    public Type getType()
    {
        return Type.MOVE;
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
