package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;

import it.sssupclient.app.exceptions.InvalidArgumentsException;

public class List implements Command {
    private String path; 

    @Override
    public void parse(String[] args) throws InvalidArgumentsException {
        if (args.length == 0) {
            this.path = ".";
        } else {
            this.path = args[0];
        }
    }

    @Override
    public void printHelp(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + getName() + " [path]");
        System.err.println(lpadding + "\tlist files at the given location or root by default");
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public void parseResponseBody(SocketChannel sc) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void exec(Scheduler scheduler) {
        // TODO Auto-generated method stub
        
        
    }
    
}
