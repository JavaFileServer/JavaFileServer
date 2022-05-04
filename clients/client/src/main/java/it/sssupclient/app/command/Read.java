package it.sssupclient.app.command;

import java.io.IOException;
import java.nio.ByteBuffer;
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

public class Read implements Command {
    private int version;
    private String path;
    private String username = null;
    private long offset, length;
    private Path dest;
    private FileChannel fout;

    @Override
    public void parse(int version, String username, String[] args) throws Exception {
        this.version = version;
        this.username = username;
        // TODO Auto-generated method stub
        if (args.length < 2) {
            throw new InvalidArgumentsException("Missing required arguments");
        }
        var file = args[0];
        var cwd = Paths.get("").toAbsolutePath();
        var dest = cwd.resolve(file);
        if (Files.exists(dest)) {
            throw new InvalidArgumentsException("File " + dest + " already exists.");
        }
        this.path = args[1];
        this.dest = dest;
        if (args.length >= 3) {
            this.length = Long.valueOf(args[2]);
        }
        if (args.length >= 4) {
            this.offset = Long.valueOf(args[3]);
        }
        // open file for creation
        this.fout = FileChannel.open(this.dest, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.SPARSE);
    }

    @Override
    public void printHelp(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + getName() + " local_path remote_path [length] [offset]");
        System.err.println(lpadding + "\t read and save in local_path the content of the specificed remote file");
    }

    @Override
    public String getName() {
        return "read";
    }

    private void consume(ByteBuffer buffer, long total_offset) throws IOException {
        this.fout.write(buffer, total_offset);
    }

    @Override
    public boolean parseResponseBody(SocketChannel sc) throws Exception {
        var status = Helpers.readShort(sc);
        var last = false;
        switch (status) {
        case 0:
            var flags = Helpers.readShort(sc);
            if ((flags & 1) != flags) {
                Helpers.panic("Bad signature: " + flags);
            }
            last = (flags & 1) == 0;
            var offset = this.version < 4 ? Helpers.readInt(sc) : Helpers.readLong(sc);
            var length = this.version < 4 ? Helpers.readInt(sc) : Helpers.readLong(sc);
            try (var wrapper = BufferManager.getBuffer()) {
                var buffer = wrapper.get();
                var read = 0L;
                while (read != length) {
                    // read
                    var remaining= length - read;
                    var toRead = (int)Math.min(remaining, buffer.remaining());
                    buffer.limit(toRead);
                    while (buffer.hasRemaining()) {
                        if (sc.read(buffer) <= 0) {
                            Helpers.panic("Bad read");
                        }
                    }
                    // consume
                    buffer.flip();
                    this.consume(buffer, offset + read);
                    buffer.clear();
                    read += toRead;
                }
            }
            break;
        case 1:
            Helpers.checkPadding(sc, 2);
            System.out.println("File not found");
            break;
        default:
            Helpers.panic("Invalid status: " + status);
        }
        return last;
    }

    public int marker;
    private void sendMsg(SocketChannel sc) {
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            buffer.putInt(this.version);
            buffer.putShort(this.getType());
            buffer.putShort((short)0);
            if (this.version >= 2) {
                buffer.put(Helpers.serializeString(this.username));
            }
            if (this.version >= 3) {
                this.marker = new Random().nextInt();
                buffer.putInt(this.marker);
            }
            buffer.put(Helpers.serializeString(this.path));
            // offset & length
            if (this.version < 4) {
                buffer.putInt((int)offset);
                buffer.putInt((int)length);
            } else {
                buffer.putLong(offset);
                buffer.putLong(length);
            }
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

    @Override
    public short getType() {
        return 1;
    }
    
}
