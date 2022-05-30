package it.sssupserver.app.filemanagers.samples;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedList;

import it.sssupserver.app.base.InvalidPathException;
import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.schedulables.SchedulableAppendCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCopyCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCreateCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCreateOrReplaceCommand;
import it.sssupserver.app.commands.schedulables.SchedulableDeleteCommand;
import it.sssupserver.app.commands.schedulables.SchedulableExistsCommand;
import it.sssupserver.app.commands.schedulables.SchedulableListCommand;
import it.sssupserver.app.commands.schedulables.SchedulableMoveCommand;
import it.sssupserver.app.commands.schedulables.SchedulableReadCommand;
import it.sssupserver.app.commands.schedulables.SchedulableTruncateCommand;
import it.sssupserver.app.commands.schedulables.SchedulableWriteCommand;
import it.sssupserver.app.filemanagers.SynchronousFileManager;

public class FlatTmpExecutor implements SynchronousFileManager {
    private String prefix = "JAVA-FILE-SERVER-";
    private java.nio.file.Path baseDir;

    static int MAX_CHUNK_SIZE = 1 << 16;

    public FlatTmpExecutor() throws Exception
    {
        this.baseDir = Files.createTempDirectory(prefix);
        System.out.println("Dase directory: " + this.baseDir);
        this.baseDir.toFile().deleteOnExit();
    }

    private boolean started;
    @Override
    public void start() throws Exception {
        if (this.started) {
            throw new Exception("Executor already started");
        }
        this.started = true;
    }

    @Override
    public void stop() throws Exception {
        if (!this.started) {
            throw new Exception("Executor was not previously started started");
        }
        this.started = false;
    }

    private void ensureFlatPath(Path path) throws InvalidPathException {
        if (!path.isFlat()) {
            throw new InvalidPathException("Path is not flat!");
        }
    }

    private void ensureRootPath(Path path) throws InvalidPathException {
        if (!path.isRoot()) {
            throw new InvalidPathException("Path is not root!");
        }
    }

    private void handleRead(SchedulableReadCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fin = FileChannel.open(filePath, StandardOpenOption.READ)) {
            var fileSz = fin.size();
            var toRead = Math.min((int)fileSz, command.getLen() != 0 ? (int)command.getLen() : MAX_CHUNK_SIZE);
            var buffer = ByteBuffer.allocate(toRead);
            fin.read(buffer, command.getBegin());
            command.reply(buffer);
        } catch (NoSuchFileException e) {
            command.notFound();
        } catch (Exception e) {
            throw e;
        }
    }

    private void handleCreateOrReplace(SchedulableCreateOrReplaceCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fout = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            var bytes = command.getData();
            fout.write(bytes);
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
            throw e;
        }
    }

    private void handleTruncate(SchedulableTruncateCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fin = FileChannel.open(filePath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            command.reply(true);
        } catch (NoSuchFileException e) {
            command.reply(false);
        } catch (Exception e) {
            throw e;
        }
    }

    private void handleExists(SchedulableExistsCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        // For security reasons do no follow symlinks
        var exists = Files.exists(filePath, LinkOption.NOFOLLOW_LINKS);
        command.reply(exists);
    }

    private void handleDelete(SchedulableDeleteCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        // For security reasons do no follow symlinks
        var success = Files.deleteIfExists(filePath);
        command.reply(success);
    }

    private void handleAppend(SchedulableAppendCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fout = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            var buffer = command.getData();
            fout.write(buffer);
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
        }
    }

    private void handleList(SchedulableListCommand command) throws Exception {
        ensureRootPath(command.getPath());
        Collection<Path> items = new LinkedList<>();
        try (var dirContent = Files.newDirectoryStream(this.baseDir)) {
            for (var entry : dirContent) {
                var r = this.baseDir.relativize(entry);
                var e = r.toString();
                var p = new Path(e);
                items.add(p);
            }
        }
        command.reply(items);
    }

    private void handleWrite(SchedulableWriteCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fout = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
            var buffer = command.getData();
            fout.write(buffer, command.getOffset());
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
        }
    }

    private void handleCreate(SchedulableCreateCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fout = FileChannel.open(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var buffer = command.getData();
            fout.write(buffer);
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
        }
    }

    private void handleCopy(SchedulableCopyCommand command) throws Exception {
        ensureFlatPath(command.getSource());
        ensureFlatPath(command.getDestination());
        var src = command.getSource().toString();
        var source = this.baseDir.resolve(src);
        var dst = command.getDestination().toString();
        var destination = this.baseDir.resolve(dst);
        try {
            Files.copy(source, destination);
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
        }
    }

    private void handleMove(SchedulableMoveCommand command) throws Exception {
        ensureFlatPath(command.getSource());
        ensureFlatPath(command.getDestination());
        var src = command.getSource().toString();
        var source = this.baseDir.resolve(src);
        var dst = command.getDestination().toString();
        var destination = this.baseDir.resolve(dst);
        try {
            Files.move(source, destination);
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
        }
    }

    @Override
    public void execute(SchedulableCommand command) throws Exception {
        if (command instanceof SchedulableReadCommand) {
            handleRead((SchedulableReadCommand)command);
        } else if (command instanceof SchedulableCreateOrReplaceCommand) {
            handleCreateOrReplace((SchedulableCreateOrReplaceCommand)command);
        } else if (command instanceof SchedulableTruncateCommand) {
            handleTruncate((SchedulableTruncateCommand)command);
        } else if (command instanceof SchedulableExistsCommand) {
            handleExists((SchedulableExistsCommand)command);
        } else if (command instanceof SchedulableDeleteCommand) {
            handleDelete((SchedulableDeleteCommand)command);
        } else if (command instanceof SchedulableAppendCommand) {
            handleAppend((SchedulableAppendCommand)command);
        } else if (command instanceof SchedulableListCommand) {
            handleList((SchedulableListCommand)command);
        } else if (command instanceof SchedulableWriteCommand) {
            handleWrite((SchedulableWriteCommand)command);
        } else if (command instanceof SchedulableCreateCommand) {
            handleCreate((SchedulableCreateCommand)command);
        } else if (command instanceof SchedulableCopyCommand) {
            handleCopy((SchedulableCopyCommand)command);
        } else if (command instanceof SchedulableMoveCommand) {
            handleMove((SchedulableMoveCommand)command);
        } else {
            throw new Exception("Unknown command");
        }
    }

    @Override
    public void scheduleExecution(SchedulableCommand command) throws Exception {
        execute(command);
    }
}
