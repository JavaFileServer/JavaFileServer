package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

public class CreateOrReplaceCommand implements Command {
    private Path path;
    byte[] data;
    private boolean sync;

    public CreateOrReplaceCommand(CreateOrReplaceCommand cmd)
    {
        this(cmd.getPath(), cmd.getData(), cmd.requireSync());
    }

    public CreateOrReplaceCommand(Path path, byte[] data, boolean sync)
    {
        this.path = path;
        this.data = data;
        this.sync = sync;
    }
    
    public CreateOrReplaceCommand(Path path, byte[] data)
    {
        this(path, data, false);
    }

    @Override
    public Type getType() {
        return Type.CREATE_OR_REPLACE;
    }

    public Path getPath()
    {
        return this.path;
    }

    public byte[] getData()
    {
        return this.data;
    }

    public boolean requireSync()
    {
        return this.sync;
    }

    @Override
    public String toString() {
        return "["+getType()+"]"+getPath();
    }
}
