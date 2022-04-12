package it.sssupserver.app.executors;

import java.nio.charset.StandardCharsets;

import it.sssupserver.app.commands.Command;
import it.sssupserver.app.commands.ReadCommand;
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

    private void handleRead(ReadCommand command, Replier replier) throws Exception
    {
        if (command.getBegin() != 0 || command.getLen() != 0)
        {
            throw new Exception("Unsupported commanf options");
        }
        var charset = StandardCharsets.UTF_8;
        var bytes = content.getBytes(charset);
        replier.replyRead(bytes);
    }

    @Override
    public void execute(Command command, Replier replier) throws Exception {
        switch (command.getType())
        {
            case READ:
                handleRead((ReadCommand)command, replier);
                break;
            default:
                throw new Exception("Unsupported command");
        }
    }

    @Override
    public void scheduleExecution(Command command, Replier replier) throws Exception
    {
        execute(command, replier);
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
