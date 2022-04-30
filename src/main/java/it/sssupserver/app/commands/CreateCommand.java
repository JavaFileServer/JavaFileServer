package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

public class CreateCommand implements Command {
    private Path path;
    byte[] data;
    private boolean sync;

    public CreateCommand(CreateCommand cmd)
    {
        this(cmd.getPath(), cmd.getData(), cmd.requireSync());
    }

    public CreateCommand(Path path, byte[] data, boolean sync)
    {
        this.path = path;
        this.data = data;
        this.sync = sync;
    }

    public CreateCommand(Path path, byte[] data)
    {
        this(path, data, false);
    }

    @Override
    public Type getType() {
        return Type.CREATE;
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
