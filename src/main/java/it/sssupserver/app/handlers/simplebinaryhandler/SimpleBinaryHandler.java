package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.executors.ReplyingExecutor;
import it.sssupserver.app.base.*;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
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

        private void scheduleCommand(Command command, DataOutputStream dout) {
            try {
                if (command instanceof ReadCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableReadCommand((ReadCommand)command, dout));
                } else if (command instanceof ExistsCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableExistsCommand((ExistsCommand)command, dout));
                } else if (command instanceof TruncateCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableTruncateCommand((TruncateCommand)command, dout));
                } else if (command instanceof CreateOrReplaceCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableCreateOrReplaceCommand((CreateOrReplaceCommand)command, dout));
                } else if (command instanceof AppendCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableAppendCommand((AppendCommand)command, dout));
                } else if (command instanceof ListCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableListCommand((ListCommand)command, dout));
                } else if (command instanceof DeleteCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableDeleteCommand((DeleteCommand)command, dout));
                } else if (command instanceof WriteCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableWriteCommand((WriteCommand)command, dout));
                } else if (command instanceof CreateCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableCreateCommand((CreateCommand)command, dout));
                } else if (command instanceof CopyCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableCopyCommand((CopyCommand)command, dout));
                } else if (command instanceof MoveCommand) {
                    executor.scheduleExecution(new SimpleBinarySchedulableMoveCommand((MoveCommand)command, dout));
                } else if (SimpleBinaryHandler.this.executor instanceof ReplyingExecutor) {
                    var replier = new SimpleBinaryHandlerReplier(dout);
                    ((ReplyingExecutor)SimpleBinaryHandler.this.executor).scheduleExecution(command, replier);
                } else {
                    dout.close();
                    throw new Exception("Cannot handle command");
                }
            } catch (Exception e) {
                String stackTrace = ""; int i=0;
                for (var st : e.getStackTrace()) {
                    stackTrace += ++i + ") " + st.toString() + "\n";
                }
                System.err.println("Error occurred while handling command [" + command + "]: " + e + "\n|> stacktrace: " + stackTrace);
            }
        }

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
                    var socket = ss.accept().socket();
                    var in = socket.getInputStream();
                    var out = socket.getOutputStream();
                    var din = new DataInputStream(in);
                    var dout = new DataOutputStream(out);
                    int version; Command command;

                    try {
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
                    } catch (Exception e) {
                        System.err.println("Error occurred while parising command: " + e);
                        continue;
                    }
                    scheduleCommand(command, dout);
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

    private Command reveiveV1Command(DataInputStream din) throws Exception
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
            default:
                throw new Exception("Unknown message type");
        }

        return command;
    }

    @Override
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

    @Override
    public void receiveAndExecuteCommand() throws Exception {
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
            int version; Command command;

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
            System.out.println("Command: " + command);
            System.out.println("Execute command...");
            if (command instanceof ReadCommand) {
                executor.execute(new SimpleBinarySchedulableReadCommand((ReadCommand)command, dout));
            } else if (SimpleBinaryHandler.this.executor instanceof ReplyingExecutor) {
                ((ReplyingExecutor)executor).execute(command, new SimpleBinaryHandlerReplier(dout));
            } else {
                dout.close();
                throw new Exception("Cannot handle command");
            }
            System.out.println("DONE!");
        }
    }

    private byte[] readBytes(DataInputStream din) throws Exception
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

    private String readString(DataInputStream din) throws Exception
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

    private ReadCommand parseV1ReadCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        String path = this.readString(din);
        int begin = din.readInt();
        int len = din.readInt();
        var cmd = new ReadCommand(new Path(path), begin, len);
        return cmd;
    }

    private CreateOrReplaceCommand parseV1CreateOrReplaceCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var data = readBytes(din);
        var cmd = new CreateOrReplaceCommand(new Path(path), data);
        return cmd;
    }

    private AppendCommand parseV1Append(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var data = readBytes(din);
        var cmd = new AppendCommand(new Path(path), data);
        return cmd;
    }

    private ExistsCommand parseV1ExistsCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var cmd = new ExistsCommand(new Path(path));
        return cmd;
    }

    private TruncateCommand parseV1TruncateCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var cmd = new TruncateCommand(new Path(path));
        return cmd;
    }

    private DeleteCommand parseV1DeleteCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var cmd = new DeleteCommand(new Path(path));
        return cmd;
    }

    private ListCommand parseV1ListCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var cmd = new ListCommand(new Path(path));
        return cmd;
    }

    private Command parseV1WriteCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var offset = din.readInt();
        var data = readBytes(din);
        var cmd = new WriteCommand(new Path(path), data, offset);
        return cmd;
    }

    private Command parseV1CreateCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var data = readBytes(din);
        var cmd = new CreateCommand(new Path(path), data);
        return cmd;
    }

    private Command parseV1CopyCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var src = this.readString(din);
        var dst = this.readString(din);
        var cmd = new CopyCommand(new Path(src), new Path(dst));
        return cmd;
    }

    private MoveCommand parseV1MoveCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var src = this.readString(din);
        var dst = this.readString(din);
        var cmd = new MoveCommand(new Path(src), new Path(dst));
        return cmd;
    }

    private void checkCategory(DataInputStream din) throws Exception
    {
        short category = din.readShort();
        if (category != 0) {
            throw new Exception("Category must be 0 for requests, foud: "+ category);
        }
    }
}
