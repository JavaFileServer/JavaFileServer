package it.sssupserver.app.executors;

import it.sssupserver.app.commands.Command;
import it.sssupserver.app.repliers.Replier;

/**
 * This dummy executor own a single string
 * that treat as it was the content of a file 
 */
public class DummyStringExecutor implements Executor {
    private String content;
    public DummyStringExecutor(String data)
    {
        this.content = data;
    }

    public DummyStringExecutor()
    {
        this("My super special file content!");
    }


    @Override
    public void execute(Command command, Replier replier) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        
    }
}
