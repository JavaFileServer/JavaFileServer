package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;

/**
 * Class representing a command provided by the user
 */
public interface Command {
    public void parse(int version, String username, String[] args) throws Exception;
    public void printHelp(String lpadding);
    public String getName();
    /**
     * Return true if this handler has completely received
     * the required response. 
     */
    public boolean parseResponseBody(SocketChannel sc) throws Exception;
    public void exec(SocketChannel sc, Scheduler scheduler);
    public short getType();
    public int getMarker();
}
