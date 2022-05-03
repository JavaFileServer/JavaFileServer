package it.sssupserver.app.handlers.simplebinaryhandler;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.users.Identity;
import it.sssupserver.app.executors.Executor;

import java.net.*;
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
                    var din = new DataInputStream(in);

                    try {
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

    public static void checkCategory(DataInputStream din) throws Exception
    {
        short category = din.readShort();
        if (category != 0) {
            throw new Exception("Category must be 0 for requests, foud: "+ category);
        }
    }
}
