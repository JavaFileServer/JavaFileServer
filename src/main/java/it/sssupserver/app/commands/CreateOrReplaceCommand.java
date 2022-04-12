package it.sssupserver.app.commands;

import it.sssupserver.app.base.Path;

public class CreateOrReplaceCommand implements Command {
    private Path path;
    byte[] data;

    public CreateOrReplaceCommand(Path path, byte[] data)
    {
        this.path = path;
        this.data = data;
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

    @Override
    public String toString() {
        return "["+getType()+"]"+getPath();
    }
}
