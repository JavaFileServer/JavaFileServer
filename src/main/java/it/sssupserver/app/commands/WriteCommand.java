package it.sssupserver.app.commands;

import java.nio.ByteBuffer;

import it.sssupserver.app.base.*;

public class WriteCommand implements Command {
    private Path path;
    private long offset;
    private ByteBuffer data;
    private boolean sync;

    public WriteCommand(WriteCommand cmd)
    {
        this(cmd.getPath(), cmd.getData(), cmd.getOffset());
    }

    public WriteCommand(Path path, ByteBuffer data, long offset, boolean sync)
    {
        this.path = path;
        this.data = data;
        this.offset = offset;
        this.sync = sync;
    }

    public WriteCommand(Path path, ByteBuffer data, long offset)
    {
        this(path, data, 0, false);
    }

    public WriteCommand(Path path, ByteBuffer data, boolean sync)
    {
        this(path, data, 0, sync);
    }

    public WriteCommand(Path path, ByteBuffer data)
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

    public long getOffset()
    {
        return this.offset;
    }

    public long getLen()
    {
        return this.data.limit() - this.data.position();
    }

    public ByteBuffer getData()
    {
        return this.data;
    }

    public boolean requireSync()
    {
        return this.sync;
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

