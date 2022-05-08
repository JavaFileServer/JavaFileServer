package it.sssupclient.app.command;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import it.sssupclient.app.BufferManager;
import it.sssupclient.app.Helpers;
import it.sssupclient.app.exceptions.InvalidArgumentsException;

public class CreateOrReplace implements Command {
    private int version;
    private String path;
    private String username = null;
    private Path src;
    private FileChannel fin;

    @Override
    public void parse(int version, String username, String[] args) throws Exception {
        this.version = version;
        this.username = username;
        if (args.length < 2) {
            throw new InvalidArgumentsException("Missing required arguments");
        }
        var file = args[0];
        if (file.equals("-")) {
            this.fin = Helpers.readAllStdin();
        } else {
            var cwd = Paths.get("").toAbsolutePath();
            var src = cwd.resolve(file);
            if (!Files.exists(src)) {
                throw new InvalidArgumentsException("File " + src + " does not exist.");
            }
            this.src = src;
            // open file for creation
            this.fin = FileChannel.open(this.src, StandardOpenOption.READ);
        }
        this.path = args[1];
    }

    @Override
    public void printHelp(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + getName() + " local_path remote_path");
        System.err.println(lpadding + "\t upload file, replace existing file");
    }

    @Override
    public String getName() {
        return "create-or-replace";
    }

    @Override
    public boolean parseResponseBody(SocketChannel sc) throws Exception {
        var status = Helpers.readByte(sc);
        switch (status) {
        case 0:
        case 1:
            Helpers.checkPadding(sc, 3);
            System.out.println("Success:" + (status == 1));
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
        return 2;
    }

    public int marker;
    private void sendMsg(SocketChannel sc) throws IOException {
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);
            if (this.version >= 3) {
                this.marker = new Random().nextInt();
                buffer.putInt(this.marker);
            }
            if (this.version >= 2) {
                buffer.put(Helpers.serializeString(this.username));
            }
            buffer.putShort(this.getType());
            buffer.putShort((short)0);
            buffer.put(Helpers.serializeString(this.path));
            // length
            var length = this.fin.size();
            if (this.version < 4) {
                buffer.putInt((int)length);
            } else {
                buffer.putLong(length);
            }
            buffer.flip();
            Helpers.writeAll(sc, buffer);
            // send all file through socket
            this.fin.transferTo(0, length, sc);
        }
    }

    @Override
    public int getMarker() {
        return this.marker;
    }    
}
