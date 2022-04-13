package it.sssupserver.app.handlers;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.repliers.Replier;
import it.sssupserver.app.base.*;

import java.net.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.io.*;


class SimpleBinaryHandlerReplier extends Thread implements Replier {
    private DataOutputStream out;
    public SimpleBinaryHandlerReplier(DataOutputStream out)
    {
        this.out = out;
    }

    @Override
    public void replyRead(byte[] data) throws Exception {
        // 20 bytes header + payload
        var bytes = new ByteArrayOutputStream(20+data.length);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(1);  // command: WRITE
        bs.writeShort(1);  // category: answer
        bs.writeShort(0);  // status: OK
        bs.writeShort(0);  // bitfield: end of answer
        bs.writeInt(0);    // offset from the beginning
        bs.writeInt(data.length);    // data length
        bs.write(data);    // data bytes
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyCreateOrReplace(boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(2);  // command: CREATE OR REPLACE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyExists(boolean exists) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(3);  // command: EXISTS
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(exists);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }

    @Override
    public void replyTruncate(boolean success) throws Exception {
        // 12 bytes header, no payload
        var bytes = new ByteArrayOutputStream(12);
        var bs = new DataOutputStream(bytes);
        // write data to buffer
        bs.writeInt(1);    // version
        bs.writeShort(4);  // command: TRUNCATE
        bs.writeShort(1);  // category: answer
        bs.writeBoolean(success);    // data bytes
        bs.write(new byte[3]);  // padding
        bs.flush();
        // now data can be sent
        bytes.writeTo(this.out);
    }
}


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
                    var socket = ss.accept().socket();
                    var in = socket.getInputStream();
                    var out = socket.getOutputStream();
                    var din = new DataInputStream(in);
                    var dout = new DataOutputStream(out);
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
                    var replier = new SimpleBinaryHandlerReplier(dout);
                    SimpleBinaryHandler.this.executor.scheduleExecution(command, replier);
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
            executor.execute(command, new SimpleBinaryHandlerReplier(dout));
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

    private Command parseV1ReadCommand(DataInputStream din) throws Exception
    {
        String path;
        int begin, len;
        short category;

        category = din.readShort();
        //System.err.println("Category: " + category);
        if (category != 0)
        {
            throw new Exception("Category must be 0 for requests, foud: "+ category);
        }
        path = this.readString(din);
        //System.err.println("Path: " + path);
        begin = din.readInt();
        //System.err.println("Begin: " + begin);
        len = din.readInt();
        //System.err.println("Len: " + len);

        var cmd = new ReadCommand(new Path(path), begin, len);
        //System.err.println("CMD = " + cmd);
        return cmd;
    }

    private Command parseV1CreateOrReplaceCommand(DataInputStream din) throws Exception
    {
        short category;

        category = din.readShort();
        //System.err.println("Category: " + category);
        if (category != 0) {
            throw new Exception("Category must be 0 for requests, foud: "+ category);
        }

        var path = this.readString(din);
        var data = readBytes(din);

        var cmd = new CreateOrReplaceCommand(new Path(path), data);
        return cmd;
    }
    
    private Command parseV1ExistsCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var cmd = new ExistsCommand(new Path(path));
        return cmd;
    }
    
    private Command parseV1TruncateCommand(DataInputStream din) throws Exception
    {
        checkCategory(din);
        var path = this.readString(din);
        var cmd = new TruncateCommand(new Path(path));
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
