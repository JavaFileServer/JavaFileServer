package it.sssupserver.app.executors.samples;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import it.sssupserver.app.executors.SynchronousExecutor;
import it.sssupserver.app.commands.schedulables.SchedulableAppendCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCreateOrReplaceCommand;
import it.sssupserver.app.commands.schedulables.SchedulableExistsCommand;
import it.sssupserver.app.commands.schedulables.SchedulableReadCommand;
import it.sssupserver.app.commands.schedulables.SchedulableTruncateCommand;

/**
 * This dummy executor own a single string
 * that treat as it was the content of a file 
 */
public class DummyStringExecutor implements SynchronousExecutor {
    private String content;
    public DummyStringExecutor(String data)
    {
        this.content = data;
    }

    public DummyStringExecutor()
    {
        this("My super special file content!");
    }

    private void handleRead(SchedulableReadCommand command) throws Exception
    {
        if (command.getBegin() != 0 || command.getLen() != 0)
        {
            throw new Exception("Unsupported commanf options");
        }
        var charset = StandardCharsets.UTF_8;
        var bytes = content.getBytes(charset);
        command.reply(ByteBuffer.wrap(bytes));
    }

    private void handleCreateOrReplace(SchedulableCreateOrReplaceCommand command) throws Exception
    {
        var data = command.getData();
        var bytes  = new byte[data.limit()];
        data.get(bytes);
        var charset = StandardCharsets.UTF_8;
        this.content = new String(bytes, charset);
        command.reply(true);
    }

    private void handleAppend(SchedulableAppendCommand command) throws Exception
    {
        var data = command.getData();
        var bytes  = new byte[data.limit()];
        data.get(bytes);
        var charset = StandardCharsets.UTF_8;
        this.content += new String(bytes, charset);
        command.reply(true);
    }
    private void handleExists(SchedulableExistsCommand command) throws Exception
    {
        command.reply(true);
    }

    private void handleTruncate(SchedulableTruncateCommand command) throws Exception
    {
        this.content = "";
        command.reply(true);
    }

    @Override
    public void execute(SchedulableCommand command) throws Exception {
        if (command instanceof SchedulableReadCommand) {
            handleRead((SchedulableReadCommand)command);
        } else if (command instanceof SchedulableExistsCommand) {
            handleExists((SchedulableExistsCommand)command);
        } else if (command instanceof SchedulableTruncateCommand) {
            handleTruncate((SchedulableTruncateCommand)command);
        } else if (command instanceof SchedulableAppendCommand) {
            handleAppend((SchedulableAppendCommand)command);
        } else if (command instanceof SchedulableCreateOrReplaceCommand) {
            handleCreateOrReplace((SchedulableCreateOrReplaceCommand)command);
        } else {
            throw new Exception("Unsupported schedulable command");
        }
    }

    @Override
    public void scheduleExecution(SchedulableCommand command) throws Exception {
        execute(command);
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
