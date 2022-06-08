package it.sssupserver.app.commands;

import it.sssupserver.app.base.*;


public class ReadCommand implements Command {
    private Path path;
    private long begin;
    private long len;

    public ReadCommand(ReadCommand cmd)
    {
        this(cmd.getPath(), cmd.getBegin(), cmd.getLen());
    }

    public ReadCommand(Path path, long begin, long len)
    {
        this.path = path;
        this.begin = begin;
        this.len = len;
    }

    public ReadCommand(Path path, int begin)
    {
        this(path, begin, 0);
    }

    public ReadCommand(Path path)
    {
        this(path, 0);
    }

    @Override
    public Type getType() {
        return Type.READ;
    }

    public Path getPath()
    {
        return this.path;
    }

    public long getBegin()
    {
        return this.begin;
    }

    public long getLen()
    {
        return this.len;
    }

    @Override
    public String toString()
    {
        return "["+getType()+"]"+getPath()+
        "("+getBegin()+";"+
            (getLen()==0 ? "END" : getBegin()+getLen() )
        +")";
    }
}
