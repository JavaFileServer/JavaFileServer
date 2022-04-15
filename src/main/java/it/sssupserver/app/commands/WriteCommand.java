package it.sssupserver.app.commands;

import it.sssupserver.app.base.*;

public class WriteCommand implements Command {
    private Path path;
    private int offset;
    private byte[] data;

    public WriteCommand(WriteCommand cmd)
    {
        this(cmd.getPath(), cmd.getData(), cmd.getOffset());
    }

    public WriteCommand(Path path, byte[] data, int offset)
    {
        this.path = path;
        this.data = data;
        this.offset = offset;
    }

    public WriteCommand(Path path, byte[] data)
    {
        this(path, data, 0);
    }

    @Override
    public Type getType() {
        return Type.WRITE;
    }

    public Path getPath()
    {
        return this.path;
    }

    public int getOffset()
    {
        return this.offset;
    }

    public int getLen()
    {
        return this.data.length;
    }

    public byte[] getData()
    {
        return this.data;
    }

    @Override
    public String toString()
    {
        return "["+getType()+"]"+getPath()+
        "("+getOffset()+";"+
            (getLen()==0 ? "END" : getOffset()+getLen() )
        +")";
    }
}

