package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

import it.sssupclient.app.BufferManager;
import it.sssupclient.app.Helpers;
import it.sssupclient.app.exceptions.InvalidArgumentsException;

public class List implements Command {
    private int version;
    private String path;
    private String username = null;

    @Override
    public short getType() {
        return 7;
    }

    @Override
    public void parse(int version, String username, String[] args) throws InvalidArgumentsException {
        this.version = version;
        this.username = username;
        if (args.length == 0) {
            this.path = ".";
        } else {
            this.path = args[0];
        }
    }

    @Override
    public void printHelp(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + getName() + " [path]");
        System.err.println(lpadding + "\tlist files at the given location or root by default");
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public boolean parseResponseBody(SocketChannel sc) {
        var status = Helpers.readShort(sc);
        String[] ans = null;
        switch (status) {
        case 0:
            Helpers.checkPadding(sc, 2);
            {
                var N = Helpers.readInt(sc);
                ans = new String[N];
                for (int i=0; i!=ans.length; ++i) {
                    ans[i] = Helpers.readString(sc);
                }
                Arrays.sort(ans);
                System.out.println("Found " + ans.length + " files:");
                for (int i=0; i!=ans.length; ++i) {
                    System.out.println(i + ") " + ans[i]);
                }
            }
            break;
        case 1:
            Helpers.checkPadding(sc, 2);
            System.out.println("Success:false");
            break;
        default:
            Helpers.panic("Invalid status: " + status);
        }
        return true;
    }

    public int marker;
    private void sendMsg(SocketChannel sc) {
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

    @Override
    public void exec(SocketChannel sc, Scheduler scheduler) {
        this.sendMsg(sc);
        scheduler.schedule(new Waiter(this.version, getType(), this, this.getMarker()));
    }
}
