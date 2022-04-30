package it.sssupserver.app.executors.samples;

import java.nio.charset.StandardCharsets;

import it.sssupserver.app.executors.ReplyingExecutor;
import it.sssupserver.app.executors.SynchronousExecutor;
import it.sssupserver.app.commands.AppendCommand;
import it.sssupserver.app.commands.Command;
import it.sssupserver.app.commands.CreateOrReplaceCommand;
import it.sssupserver.app.commands.ExistsCommand;
import it.sssupserver.app.commands.ReadCommand;
import it.sssupserver.app.commands.TruncateCommand;
import it.sssupserver.app.commands.schedulables.SchedulableAppendCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCreateOrReplaceCommand;
import it.sssupserver.app.commands.schedulables.SchedulableExistsCommand;
import it.sssupserver.app.commands.schedulables.SchedulableReadCommand;
import it.sssupserver.app.commands.schedulables.SchedulableTruncateCommand;
import it.sssupserver.app.repliers.Replier;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * This dummy executor own a single string
 * that treat as it was the content of a file 
 */
public class DummyMapExecutor implements ReplyingExecutor, SynchronousExecutor {
    private Map<String,String> content;

    public DummyMapExecutor()
    {
        this.content = new ConcurrentHashMap<>();
    }

    private void handleRead(ReadCommand command, Replier replier) throws Exception
    {
        if (command.getBegin() != 0 || command.getLen() != 0)
        {
            throw new Exception("Unsupported command options");
        }
        var file = command.getPath().toString();
        var data = this.content.get(file);
        if (data != null) {
            var charset = StandardCharsets.UTF_8;
            var bytes = data.getBytes(charset);
            replier.replyRead(bytes);
        } else {
            replier.replyReadNotFound();
        }
    }

    private void handleRead(SchedulableReadCommand command) throws Exception
    {
        if (command.getBegin() != 0 || command.getLen() != 0)
        {
            throw new Exception("Unsupported command options");
        }
        var file = command.getPath().toString();
        var data = this.content.get(file);
        if (data != null) {
            var charset = StandardCharsets.UTF_8;
            var bytes = data.getBytes(charset);
            command.reply(bytes);
        } else {
            command.notFound();
        }
    }

    private void handleCreateOrReplace(CreateOrReplaceCommand command, Replier replier) throws Exception
    {
        var file = command.getPath().toString();
        var data = command.getData();
        var bytes  = new byte[data.limit()];
        data.get(bytes);
        var charset = StandardCharsets.UTF_8;
        this.content.put(file, new String(bytes, charset));
        replier.replyCreateOrReplace(true);
    }

    private void handleCreateOrReplace(SchedulableCreateOrReplaceCommand command) throws Exception
    {
        var file = command.getPath().toString();
        var data = command.getData();
        var bytes  = new byte[data.limit()];
        data.get(bytes);
        var charset = StandardCharsets.UTF_8;
        this.content.put(file, new String(bytes, charset));
        command.reply(true);
    }

    private void handleAppend(AppendCommand command, Replier replier) throws Exception
    {
        var data = command.getData();
        var bytes  = new byte[data.limit()];
        data.get(bytes);
        var charset = StandardCharsets.UTF_8;
        var file = command.getPath().toString();
        var ans = this.content.computeIfPresent(file, (BiFunction<String,String,String>)(String K, String V) -> {
            return V + new String(bytes, charset);
        }) != null;
        replier.replyAppend(ans);
    }

    private void handleAppend(SchedulableAppendCommand command) throws Exception
    {
        var data = command.getData();
        var bytes  = new byte[data.limit()];
        data.get(bytes);
        var charset = StandardCharsets.UTF_8;
        var file = command.getPath().toString();
        var ans = this.content.computeIfPresent(file, (BiFunction<String,String,String>)(String K, String V) -> {
            return V + new String(bytes, charset);
        }) != null;
        command.reply(ans);
    }

    private void handleExists(ExistsCommand command, Replier replier) throws Exception
    {
        var file = command.getPath().toString();
        var ans = this.content.containsKey(file);
        replier.replyExists(ans);
    }

    private void handleExists(SchedulableExistsCommand command) throws Exception
    {
        var file = command.getPath().toString();
        var ans = this.content.containsKey(file);
        command.reply(ans);
    }

    private void handleTruncate(TruncateCommand command, Replier replier) throws Exception
    {
        var file = command.getPath().toString();
        boolean ans = this.content.replace(file, "") != null;
        replier.replyTruncate(ans);
    }

    private void handleTruncate(SchedulableTruncateCommand command) throws Exception
    {
        var file = command.getPath().toString();
        boolean ans = this.content.replace(file, "") != null;
        command.reply(ans);
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
    public void scheduleExecution(Command command, Replier replier) throws Exception
    {
        execute(command, replier);
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
