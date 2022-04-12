package it.sssupserver.app.commands;

import it.sssupserver.app.base.*;


public class ReadCommand implements Command {
    private Path path;
    private int begin;
    private int len;

    public ReadCommand(Path path, int begin, int len)
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

    public int getBegin()
    {
        return this.begin;
    }

    public int getLen()
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
