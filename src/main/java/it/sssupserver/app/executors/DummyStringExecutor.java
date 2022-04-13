package it.sssupserver.app.executors;

import java.nio.charset.StandardCharsets;

import it.sssupserver.app.commands.AppendCommand;
import it.sssupserver.app.commands.Command;
import it.sssupserver.app.commands.CreateOrReplaceCommand;
import it.sssupserver.app.commands.ExistsCommand;
import it.sssupserver.app.commands.ReadCommand;
import it.sssupserver.app.commands.TruncateCommand;
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

    private void handleCreateOrReplace(CreateOrReplaceCommand command, Replier replier) throws Exception
    {
        var bytes = command.getData();
        var charset = StandardCharsets.UTF_8;
        this.content = new String(bytes, charset);
        replier.replyCreateOrReplace(true);
    }

    private void handleAppend(AppendCommand command, Replier replier) throws Exception
    {
        var bytes = command.getData();
        var charset = StandardCharsets.UTF_8;
        this.content += new String(bytes, charset);
        replier.replyAppend(true);
    }

    private void handleExists(ExistsCommand command, Replier replier) throws Exception
    {
        // by default file always exists
        replier.replyExists(true);
    }

    private void handleTruncate(TruncateCommand command, Replier replier) throws Exception
    {
        this.content = "";
        replier.replyTruncate(true);
    }

    @Override
    public void execute(Command command, Replier replier) throws Exception {
        switch (command.getType())
        {
            case READ:
                handleRead((ReadCommand)command, replier);
                break;
            case CREATE_OR_REPLACE:
                handleCreateOrReplace((CreateOrReplaceCommand)command, replier);
                break;
            case EXISTS:
                handleExists((ExistsCommand)command, replier);
                break;
            case TRUNCATE:
                handleTruncate((TruncateCommand)command, replier);
                break;
            case APPEND:
                handleAppend((AppendCommand)command, replier);
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

    private boolean started;

    @Override
    public void start() throws Exception {
        if (this.started)
        {
            throw new Exception("Executor already started");
        }
        this.started = true;
    }

    @Override
    public void stop() throws Exception {
        if (!this.started)
        {
            throw new Exception("Executor not started");
        }
        this.started = false;
    }
}
