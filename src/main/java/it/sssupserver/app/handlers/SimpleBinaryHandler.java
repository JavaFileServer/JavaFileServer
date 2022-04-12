package it.sssupserver.app.handlers;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.repliers.Replier;
import it.sssupserver.app.base.*;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;


class SimpleBinaryHandlerReplier implements Replier {
    private DataOutput out;
    public SimpleBinaryHandlerReplier(DataOutput out)
    {
        this.out = out;
    }

    @Override
    public void replyRead(byte[] data) throws Exception {
        out.write(data);
    }
}


public class SimpleBinaryHandler implements RequestHandler {

    private int port;
    private boolean started = false;

    public SimpleBinaryHandler() throws Exception
    {
        this(5050);
    }

    public SimpleBinaryHandler(int port) throws Exception
    {
        if (!(0 < port || port < (1<<16)))
        {
            throw new Exception("Invalid port number: " + port);
        }
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public void stop() throws Exception {
        throw new Exception("Not implemented");
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
                command = parseReadCommand(din);
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

    private String readString(DataInputStream din) throws Exception
    {
        var len = din.readInt();
        if (len <= 0)
        {
            throw new Exception("Malformed input!");
        }

        var bytes = new byte[len];
        din.readFully(bytes);
        var recover = new String(bytes, StandardCharsets.UTF_8);

        return recover;
    }

    private Command parseReadCommand(DataInputStream din) throws Exception
    {
        String path;
        int begin, len;
        short category;

        category = din.readShort();
        //System.err.println("Category: " + category);
        if (category != 1)
        {
            throw new Exception("Category must be 1 for requests, foud: "+ category);
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
}
