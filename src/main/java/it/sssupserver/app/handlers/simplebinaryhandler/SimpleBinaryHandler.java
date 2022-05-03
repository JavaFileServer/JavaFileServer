package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.users.Identity;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.executors.ReplyingExecutor;
import it.sssupserver.app.executors.SynchronousExecutor;
import it.sssupserver.app.base.*;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.io.*;


public class SimpleBinaryHandler implements RequestHandler {

    private int port;
    private boolean started = false;
    private Executor executor;

    public SimpleBinaryHandler(Executor executor) throws Exception
    {
        this(executor, 5050);
    }

    public SimpleBinaryHandler(Executor executor, int port) throws Exception
    {
        if (!(0 < port || port < (1<<16)))
        {
            throw new Exception("Invalid port number: " + port);
        }
        this.port = port;
        this.executor = executor;
    }

    class Listener extends Thread {

        @Override
        public void run()
        {
            try (var ss = ServerSocketChannel.open())
            {
                ss.bind(new InetSocketAddress(SimpleBinaryHandler.this.port));
                while (true)
                {
                    // See doc
                    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/channels/SocketChannel.html
                    // Not optimal but at least work
                    var schannel = ss.accept();
                    var socket = schannel.socket();
                    var in = socket.getInputStream();
                    var out = socket.getOutputStream();
                    var din = new DataInputStream(in);

                    try {
                        Command command;
                        SchedulableCommand schedulable;
                        int version;
                        Identity user = null;
                        int marker = 0;
                        short type = 0;
                        switch (version = din.readInt()) {
                        case 1: // no special parameters
                            break;
                        case 2: // username available
                            {
                                var username = readString(din);
                                var hash = username.hashCode();
                                user = new Identity(username, hash);
                            }
                            break;
                        case 3: // username and marker
                            {
                                marker = din.readInt();
                                var username = readString(din);
                                var hash = username.hashCode();
                                user = new Identity(username, hash);
                            }
                            break;
                        default:
                            throw new Exception("Unknown message version: " + version);
                        }

                        switch (type = din.readShort()) {
                            case 1:
                            SimpleBinarySchedulableReadCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 2:
                            SimpleBinarySchedulableCreateOrReplaceCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 3:
                            SimpleBinarySchedulableWriteCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 4:
                            SimpleBinarySchedulableTruncateCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 5:
                            SimpleBinarySchedulableAppendCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 6:
                            SimpleBinarySchedulableDeleteCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 7:
                            SimpleBinarySchedulableListCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 8:
                            SimpleBinarySchedulableWriteCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 9:
                            SimpleBinarySchedulableCreateCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 10:
                            SimpleBinarySchedulableCopyCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 11:
                            SimpleBinarySchedulableMoveCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        case 12:
                            SimpleBinarySchedulableMkdirCommand.handle(executor, schannel, din, version, user, marker);
                            break;
                        default:
                            throw new Exception("Unknown message type: " + type);
                        }

                    } catch (Exception e) {
                        System.err.println("Error occurred while parsing command: " + e);
                        String stackTrace = ""; int i=0;
                        for (var st : e.getStackTrace()) {
                            stackTrace += ++i + ") " + st.toString() + "\n";
                        }
                        System.err.println("Error occurred while scheduling command" + e.getMessage() + "\n|> stacktrace: " + stackTrace);
                    }
                }
            } catch (ClosedByInterruptException e) {
                System.err.println("Listener interrupted!");
            } catch (Exception e) {
                System.err.println("Listener failed to initialize " + e);
            }
        }
    }

    private Listener worker;
    @Override
    public void start() throws Exception {
        var listener = new Listener();
        listener.start();
        this.worker = listener;
        System.out.println("Worker started");
    }


    @Override
    public void stop() throws Exception {
        if (worker == null)
        {
            throw new Exception("No listener working");
        }
        System.out.println("Interrupt worker...");
        worker.interrupt();
        System.out.println("Waiting for join...");
        worker.join();
        System.out.println("Worker terminated!");
    }

    private static Command reveiveV1Command(DataInputStream din) throws Exception
    {
        short type;
        type = din.readShort();
        //System.err.println("Type: " + type);
        Command command;

        switch (type)
        {
            case 1:
                command = parseV1ReadCommand(din);
                break;
            case 2:
                command = parseV1CreateOrReplaceCommand(din);
                break;
            case 3:
                command = parseV1ExistsCommand(din);
                break;
            case 4:
                command = parseV1TruncateCommand(din);
                break;
            case 5:
                command = parseV1Append(din);
                break;
            case 6:
                command = parseV1DeleteCommand(din);
                break;
            case 7:
                command = parseV1ListCommand(din);
                break;
            case 8:
                command = parseV1WriteCommand(din);
                break;
            case 9:
                command = parseV1CreateCommand(din);
                break;
            case 10:
                command = parseV1CopyCommand(din);
                break;
            case 11:
                command = parseV1MoveCommand(din);
                break;
            case 12:
                command = parseV1MkdirCommand(din);
                break;
            default:
                throw new Exception("Unknown message type");
        }

        return command;
    }

    public Command receiveCommand() throws Exception {
        if (this.started) {
            throw new Exception("Incoerent usage");
        }

        Command command;

        System.out.println("Listening on port " + port);
        try (
            var ss = new ServerSocket(this.port);
            var sck = ss.accept();
            var in = sck.getInputStream();
            var out = sck.getOutputStream();
            var din = new DataInputStream(in);
            var dout = new DataOutputStream(out)
            )
        {
            System.out.println("Request received! Parsing...");
            int version;
            version = din.readInt();
            //System.err.println("Version: " + version);
            switch (version)
            {
                case 1:
                    command = reveiveV1Command(din);
                    break;
                default:
                    throw new Exception("Unknown message version");
            }

        }

        return command;
    }

    public static byte[] readBytes(DataInputStream din) throws Exception
    {
        var len = din.readInt();
        if (len < 0)
        {
            throw new Exception("Malformed input!");
        }

        var bytes = new byte[len];
        din.readFully(bytes);
        return bytes;
    }

    public static String readString(DataInputStream din) throws Exception
    {
        var bytes = readBytes(din);
        var recover = new String(bytes, StandardCharsets.UTF_8);

        return recover;
    }

    public static byte[] serializeString(String string) throws Exception
    {
        var data = string.getBytes(StandardCharsets.UTF_8);
        var bytes = new ByteArrayOutputStream(4 + data.length);
        var bs = new DataOutputStream(bytes);
        bs.writeInt(data.length);
        bs.write(data);
        bs.flush();
        return bytes.toByteArray();
    }

    private static ReadCommand parseV1ReadCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        String path = readString(din);
        int begin = din.readInt();
        int len = din.readInt();
        var cmd = new ReadCommand(new Path(path), begin, len);
        return cmd;
    }

    private static CreateOrReplaceCommand parseV1CreateOrReplaceCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var data = readBytes(din);
        var cmd = new CreateOrReplaceCommand(new Path(path), ByteBuffer.wrap(data));
        return cmd;
    }

    private static AppendCommand parseV1Append(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var data = readBytes(din);
        var cmd = new AppendCommand(new Path(path), ByteBuffer.wrap(data));
        return cmd;
    }

    private static ExistsCommand parseV1ExistsCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var cmd = new ExistsCommand(new Path(path));
        return cmd;
    }

    private static TruncateCommand parseV1TruncateCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var cmd = new TruncateCommand(new Path(path));
        return cmd;
    }

    private static DeleteCommand parseV1DeleteCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var cmd = new DeleteCommand(new Path(path));
        return cmd;
    }

    private static ListCommand parseV1ListCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var cmd = new ListCommand(new Path(path));
        return cmd;
    }

    private static WriteCommand parseV1WriteCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var offset = din.readInt();
        var data = readBytes(din);
        var cmd = new WriteCommand(new Path(path), ByteBuffer.wrap(data), offset);
        return cmd;
    }

    private static CreateCommand parseV1CreateCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var data = readBytes(din);
        var cmd = new CreateCommand(new Path(path), ByteBuffer.wrap(data));
        return cmd;
    }

    private static CopyCommand parseV1CopyCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var src = readString(din);
        var dst = readString(din);
        var cmd = new CopyCommand(new Path(src), new Path(dst));
        return cmd;
    }

    private static MoveCommand parseV1MoveCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var src = readString(din);
        var dst = readString(din);
        var cmd = new MoveCommand(new Path(src), new Path(dst));
        return cmd;
    }

    private static MkdirCommand parseV1MkdirCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = readString(din);
        var cmd = new MkdirCommand(new Path(path));
        return cmd;
    }

    public static void checkCategory(DataInputStream din) throws Exception
    {
        short category = din.readShort();
        if (category != 0) {
            throw new Exception("Category must be 0 for requests, foud: "+ category);
        }
    }
}
