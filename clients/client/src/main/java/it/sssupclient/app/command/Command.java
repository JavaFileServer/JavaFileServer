package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;

/**
 * Class representing a command provided by the user
 */
public abstract class Command {
    public abstract void parse(int version, String username, String[] args) throws Exception;
    public abstract void printHelp(String lpadding);
    public abstract String getName();
    /**
     * Return true if this handler has completely received
     * the required response. 
     */
    public abstract boolean parseResponseBody(SocketChannel sc) throws Exception;
    public abstract void exec(SocketChannel sc, Scheduler scheduler) throws Exception;
    public abstract short getType();
    public abstract int getMarker();
    // Did the command execution successfully completed?
    protected boolean success = false;
    public boolean successful() {
        return this.success;
    }
}
