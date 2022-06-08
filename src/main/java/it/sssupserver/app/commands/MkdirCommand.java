package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

public class MkdirCommand implements Command {
    private Path path;

    public MkdirCommand(MkdirCommand cmd)
    {
        this(cmd.getPath());
    }

    public MkdirCommand(Path path)
    {
        this.path = path;
    }
    
    @Override
    public Type getType() {
        return Type.MKDIR;
    }

    public Path getPath()
    {
        return this.path;
    }

    @Override
    public String toString() {
        return "["+getType()+"]"+getPath();
    }
}
