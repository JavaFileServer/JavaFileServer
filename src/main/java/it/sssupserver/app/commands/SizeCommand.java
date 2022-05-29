package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

public class SizeCommand {
    private Path path;

    public SizeCommand(SizeCommand cmd)
    {
        this(cmd.getPath());
    }

    public SizeCommand(Path path)
    {
        this.path = path;
    }

    public Type getType()
    {
        return Type.SIZE;
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
