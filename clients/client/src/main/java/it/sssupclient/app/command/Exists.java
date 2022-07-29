package it.sssupclient.app.command;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

import it.sssupclient.app.BufferManager;
import it.sssupclient.app.Helpers;
import it.sssupclient.app.exceptions.InvalidArgumentsException;

public class Exists extends Command {
    private int version;
    private String path;
    private String username = null;

    @Override
    public void parse(int version, String username, String[] args) throws Exception {
        this.version = version;
        this.username = username;
        if (args.length < 1) {
            throw new InvalidArgumentsException("Missing required arguments");
        }
        this.path = args[0];
    }

    @Override
    public void printHelp(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + getName() + " remote_path");
        System.err.println(lpadding + "\t test existence of remote file");
    }

    @Override
    public String getName() {
        return "exists";
    }

    @Override
    public boolean parseResponseBody(SocketChannel sc, ArrayList<String> response) throws Exception {
        var status = Helpers.readByte(sc);
        switch (status) {
        case 0:
        case 1:
            Helpers.checkPadding(sc, 3);
            System.out.println("Success:" + (this.success = status == 1));
            break;
        default:
            Helpers.panic("Invalid status: " + status);
        }
        return true;
    }

    @Override
    public void exec(SocketChannel sc, Scheduler scheduler) throws Exception {
        this.sendMsg(sc);
        scheduler.schedule(new Waiter(this.version, getType(), this, this.getMarker()));
    }

    @Override
    public short getType() {
        return 3;
    }

    public int marker;
    private void sendMsg(SocketChannel sc) throws IOException {
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);
            if (this.version >= 3) {
                this.marker = new Random(Instant.now().toEpochMilli()).nextInt();
                buffer.putInt(this.marker);
            }
            if (this.version >= 2) {
                buffer.put(Helpers.serializeString(this.username));
            }
            buffer.putShort(this.getType());
            buffer.putShort((short)0);
            buffer.put(Helpers.serializeString(this.path));
            buffer.flip();
            Helpers.writeAll(sc, buffer);
        }
    }

    @Override
    public int getMarker() {
        return this.marker;
    }
}
