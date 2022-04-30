package it.sssupserver.app.executors.samples;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.*;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.exceptions.InvalidIdentityException;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

public class UserTreeExecutor implements Executor {
    private java.nio.file.Path baseDir;
    private String unknown_users = "unknown";
    // prefix identifying
    private String userdir_prefix = "u";
    private ExecutorService pool;
    private ConcurrentMap<java.nio.file.Path, java.nio.channels.FileChannel> filemap = new ConcurrentSkipListMap<>();

    static int MAX_CHUNK_SIZE = 2 << 16;

    //
    private ConcurrentMap<java.nio.file.Path, Boolean> dirmap = new ConcurrentSkipListMap<>();
    /**
     * Each user has access to a different folder.
     */
    private java.nio.file.Path userDir(Identity user) throws InvalidIdentityException, ApplicationException {
        var uDir = this.baseDir.resolve(user == null ? unknown_users : userdir_prefix + user.getId());
        var error = new Object(){
            public IOException exception;
        };
        if (dirmap.computeIfAbsent(uDir, (dir) -> {
                if (!Files.exists(uDir, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.createDirectory(dir);
                    } catch (IOException e) {
                        error.exception = e;
                        return null;
                    }
                }
                return true;
            }) == null) {
            throw new ApplicationException(error.exception);
        };
        return uDir;
    }

    public UserTreeExecutor() throws Exception
    {
        String prefix = "JAVA-UserTreeExecutor-SERVER-";
        this.baseDir = Files.createTempDirectory(prefix);
        System.out.println("Dase directory: " + this.baseDir);
        this.baseDir.toFile().deleteOnExit();
    }

    private void handleRead(SchedulableReadCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.computeIfPresent(path, (p, fin) -> {
                try {
                    var fileSz = fin.size();
                    var toRead = Math.min((int)fileSz, command.getLen() != 0 ? command.getLen() : MAX_CHUNK_SIZE);
                    var bytes = new byte[toRead];
                    var buffer = ByteBuffer.wrap(bytes);
                    fin.read(buffer, command.getBegin());
                    try { command.reply(bytes); } catch (Exception ee) { }
                } catch (Exception e) {
                    try { fin.close(); } catch (Exception ee) { }
                    return null;
                }
                return fin;
            }) == null) {
                try { command.notFound(); } catch (Exception ee) { }
            }
        });
    }

    private void handleCreateOrReplace(SchedulableCreateOrReplaceCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.compute(path, (p, fout) -> {
                if (fout == null) {
                    try {
                        fout = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                    } catch (Exception e) {
                        try { command.reply(false); } catch (Exception ee) { }
                        return null;
                    }
                }
                try {
                    var bytes = command.getData();
                    var buffer = ByteBuffer.wrap(bytes);
                    fout.truncate(0).write(buffer);
                    try { command.reply(true); } catch (Exception ee) { }
                } catch (Exception e) {
                    try { command.reply(false); } catch (Exception ee) { }
                    try { fout.close(); } catch (Exception ee) { }
                    return null;
                }
                return fout;
            }) == null) {
                try { command.reply(false); } catch (Exception e) { }
            }
        });
    }

    private void handleTruncate(SchedulableTruncateCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.compute(path, (p, fout) -> {
                try {
                    if (fout == null) {
                        fout = FileChannel.open(path,  StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                    } else {
                        fout.truncate(0);
                    }
                } catch (IOException e) {
                    try { command.reply(false); } catch (Exception ee) { }
                    try { fout.close(); } catch (Exception ee) { }
                    return null;
                }
                try { command.reply(true); } catch (Exception ee) { }
                return fout;
            }) == null) {
                try { command.reply(false); } catch (Exception e) { }
            }
        });
    }

    private void handleExists(SchedulableExistsCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        throw new ApplicationException("NOT IMPLEMENTED");
    }

    private void handleDelete(SchedulableDeleteCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.compute(path, (p, fout) -> {
                try {
                    if (fout != null) {
                        fout.close();
                    }
                    var success = Files.deleteIfExists(path);
                    try { command.reply(success); } catch (Exception ee) { }
                } catch (IOException e) {
                    try { command.reply(false); } catch (Exception ee) { }
                }
                return null;
            }) == null) {
                try { command.reply(false); } catch (Exception e) { }
            }
        });
    }

    private void handleAppend(SchedulableAppendCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.computeIfPresent(path, (p, fout) -> {
                try {
                    var bytes = command.getData();
                    var buffer = ByteBuffer.wrap(bytes);
                    fout.position(fout.size()).write(buffer);
                    try { command.reply(true); } catch (Exception ee) { }
                } catch (IOException e) {
                    try { command.reply(false); } catch (Exception ee) { }
                    try { fout.close(); } catch (Exception ee) { }
                    return null;
                }
                return fout;
            }) == null) {
                try { command.reply(false); } catch (Exception e) { }
            }
        });
    }

    private void handleList(SchedulableListCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            Collection<Path> items = new LinkedList<>();
            try (var dirContent = Files.newDirectoryStream(path)) {
                for (var entry : dirContent) {
                    var r = this.baseDir.relativize(entry);
                    var e = r.toString();
                    var p = new Path(e);
                    items.add(p);
                }
                try { command.reply(items); } catch (Exception ee) { }
            } catch (Exception e) {
                try { command.notFound(); } catch (Exception ee) { }
            }
        });
    }

    private void handleWrite(SchedulableWriteCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.computeIfPresent(path, (p, fout) -> {
                try {
                    var bytes = command.getData();
                    var buffer = ByteBuffer.wrap(bytes);
                    fout.position(command.getOffset()).write(buffer);
                    try { command.reply(true); } catch (Exception ee) { }
                } catch (IOException e) {
                    try { command.reply(false); } catch (Exception ee) { }
                }
                return fout;
            }) == null) {
                try { command.reply(false); } catch (Exception e) { }
            }
        });
    }

    private void handleCreate(SchedulableCreateCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            if (this.filemap.computeIfAbsent(path, (p) -> {
                FileChannel fout = null;
                try {
                    fout = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                } catch (IOException e) {
                    try { command.reply(false); } catch (Exception ee) { }
                    return null;
                }
                try {
                    var bytes = command.getData();
                    var buffer = ByteBuffer.wrap(bytes);
                    fout.truncate(0).write(buffer);
                    try { command.reply(true); } catch (Exception ee) { }
                } catch (Exception e) {
                    try { command.reply(false); } catch (Exception ee) { }
                }
                return fout;
            }) == null) {
                try { command.reply(false); } catch (Exception e) { }
            }
        });
    }

    private void handleCopy(SchedulableCopyCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        throw new ApplicationException("NOT IMPLEMENTED");
    }

    private void handleMove(SchedulableMoveCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        throw new ApplicationException("NOT IMPLEMENTED");
    }

    private void handleMkdir(SchedulableMkdirCommand command) throws ApplicationException {
        var user = command.getUser();
        var uDir = userDir(user);
        var path = java.nio.file.Path.of(uDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            try {
                Files.createDirectory(path);
                try { command.reply(true); } catch (Exception ee) { }
            } catch (IOException e) {
                try { command.reply(false); } catch (Exception ee) { }
            }
        });
    }


    private boolean started;
    @Override
    public void scheduleExecution(SchedulableCommand command) throws Exception {
        // TODO Auto-generated method stub
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
        } else if (command instanceof SchedulableMkdirCommand) {
            handleMkdir((SchedulableMkdirCommand)command);
        } else {
            throw new Exception("Unknown command");
        }
    }

    @Override
    public void start() throws Exception {
        if (this.started) {
            throw new Exception("Executor already started");
        }
        this.started = true;
        this.pool = Executors.newCachedThreadPool();
    }

    @Override
    public void stop() throws Exception {
        this.pool.shutdown();
        if (!this.started) {
            throw new Exception("Executor was not previously started started");
        }
        this.started = false;
    }
}
