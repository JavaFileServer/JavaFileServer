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

public class Write implements Command {
    private int version;
    private String path;
    private String username = null;
    private Path src;
    private FileChannel fin;
    private long offset, length;

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
        if (args.length >= 3) {
            this.offset = Long.valueOf(args[2]);
            if (args.length >= 4) {
                this.length = Long.valueOf(args[3]);
            }
        }
    }

    @Override
    public void printHelp(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + getName() + " local_path remote_path [offset] [length]");
        System.err.println(lpadding + "\t write length (or all) bytes from local_path to the remote file starting ar offset (or beginning). 0 length means file length. If length in grater than file size it is shrinked to file size.");
    }

    @Override
    public String getName() {
        return "write";
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
        return 8;
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
            buffer.putShort(this.getType());
            buffer.putShort((short)0);
            if (this.version >= 2) {
                buffer.put(Helpers.serializeString(this.username));
            }
            buffer.put(Helpers.serializeString(this.path));
            // length
            var length = this.length == 0 ? this.fin.size() : Math.min(this.length, this.fin.size());
            if (this.version < 4) {
                buffer.putInt((int)this.offset);
                buffer.putInt((int)length);
            } else {
                buffer.putLong(this.offset);
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
