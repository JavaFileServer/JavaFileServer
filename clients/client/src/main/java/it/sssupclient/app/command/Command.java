package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;

import it.sssupclient.app.exceptions.InvalidArgumentsException;

/**
 * Class representing a command provided by the user
 */
public interface Command {
    public void parse(String[] args) throws InvalidArgumentsException;
    public void printHelp(String lpadding);
    public String getName();
    public void parseResponseBody(SocketChannel sc);
    public void exec(Scheduler scheduler);
}
