package it.sssupserver.app.executors.samples;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.schedulables.*;
import it.sssupserver.app.exceptions.ApplicationException;
import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.users.Identity;

public class UserTreeExecutor implements Executor {
    private java.nio.file.Path baseDir;
    private String unknown_users = "unknown";
    // prefix identifying
    private String userdir_prefix = "u";
    private ExecutorService pool;
    // Will handle a single periodic task that will
    // close all FileChannel(s) owned by users who
    // have not performed any operation in the last
    // cleanup_delay_ms*repeat_after time unit.
    // The task will be runned every repeat_after
    // time unit.
    private ScheduledExecutorService timedPoll;
    // interval after which the 'cleanup thread'
    private long cleanup_delay_ms = 60*1000;
    private long repeat_after = 5;

    static int MAX_CHUNK_SIZE = 1 << 16;
    // At most WORKER_THREAD_COUNT will be used
    // to handle client requests
    static int WORKER_THREAD_COUNT = 8;

    // This class represent the subtree of the machine FS
    // 'owned' by a certain user.
    // It is used internally by this Executor to maintain
    // data associated to a specific user
    private class UserFS {
        // null for unauthenticated users, otherwise
        // the user Id
        public final Optional<Long> userId;
        // path to the root folder for the specified user
        public final java.nio.file.Path userDir;
        // to effectively support risky operations like MOVE
        public final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        // map paths to files
        public final ConcurrentMap<java.nio.file.Path, java.nio.channels.FileChannel> filemap = new ConcurrentHashMap<>();
        // time
        public Instant lastAccess = Instant.now();

        public class LockWrapper implements AutoCloseable {
            private final Lock lock;
            LockWrapper(Lock lock) {
                this.lock = lock;
                this.lock.lock();
            }

            private boolean closed;
            @Override
            public void close() {
                if (!closed) {
                    lastAccess = Instant.now();
                    this.lock.unlock();
                    closed = true;
                }
            }
        }

        public LockWrapper readLock() {
            return new LockWrapper(lock.readLock());
        }

        public LockWrapper writeLock() {
            return new LockWrapper(lock.writeLock());
        }

        public UserFS(Identity user) throws IOException {
            this.userId = Optional.ofNullable(user != null ? user.getIdOrNull() : null);
            this.userDir = getPathFromUser(user);
        }

        public UserFS() throws IOException {
            this(null);
        }
    }

    private java.nio.file.Path getPathFromUser(Identity user) throws IOException {
        var uDir = this.baseDir.resolve(user == null ? unknown_users : userdir_prefix + user.getIdOrNull());
        if (!Files.exists(uDir, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(uDir);
        }
        return uDir;
    }

    // map user IDs to their root folder
    private ConcurrentMap<Identity, UserFS> userFS = new ConcurrentSkipListMap<>();
    private UserFS defaultFS;

    private UserFS getUserFS(Identity user) throws ApplicationException {
        if (defaultFS == null) {
            try {
                defaultFS = new UserFS();
            } catch (IOException e) {
                throw new ApplicationException("Error creating deafultFS", e);
            }
        }
        try {
            if (user == null || !user.isValid()) {
                return defaultFS;
            } else {
                var flag = new Object(){
                    public IOException error = null;
                };
                var ans = userFS.computeIfAbsent(user, (u) -> {
                    UserFS fs;
                    try {
                        fs = new UserFS(user);
                    } catch (IOException e) {
                        flag.error = e;
                        fs = null;
                    }
                    return fs;
                });
                if (ans == null) {
                    throw new ApplicationException(flag.error);
                }
                return ans;
            }
        } catch (Exception e) {
            throw new ApplicationException("Inconsistent state", e);
        }
    }

    public UserTreeExecutor() throws Exception
    {
        String prefix = "JAVA-UserTreeExecutor-SERVER-";
        this.baseDir = Files.createTempDirectory(prefix);
        System.out.println("Dase directory: " + this.baseDir);
        this.baseDir.toFile().deleteOnExit();
    }

    public UserTreeExecutor(java.nio.file.Path dir) throws Exception
    {
        if (Files.exists(dir)) {
            if (!Files.isDirectory(dir)) {
                throw new Exception("'" + dir + "' is not a directory");
            }
        } else {
            Files.createDirectories(dir);
        }
        this.baseDir = dir;
        System.out.println("Dase directory: " + this.baseDir);
    }

    private Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
    private class BufferWrapper implements AutoCloseable {
        private ByteBuffer buffer;
        public BufferWrapper(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        public ByteBuffer get() {
            return this.buffer;
        }

        @Override
        public void close() throws Exception {
            buffer.clear();
            bufferQueue.add(buffer);
        }
    }
    private BufferWrapper getBuffer() {
        var buffer = bufferQueue.poll();
        return new BufferWrapper(buffer == null
                ? ByteBuffer.allocateDirect(MAX_CHUNK_SIZE)
                : buffer);
    }

    public void handle(SchedulableReadCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean found = false;
            };
            try (var lock = uFS.readLock()) {
                if (uFS.filemap.compute(path, (p, fin) -> {
                    try {
                        if (fin == null) {
                            fin = FileChannel.open(p, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                        }
                        flag.found = true;
                        fin.position(command.getBegin());
                        var remainder = fin.size() - fin.position();
                        var toRead = command.getLen() != 0 ? command.getLen() : remainder;
                        long read = 0;
                        try (var wrapper = getBuffer()) {
                            var buffer = wrapper.get();
                            do {
                                buffer.limit((int)Math.min(buffer.capacity(), toRead));
                                var tmp = fin.read(buffer, command.getBegin() + read);
                                if (tmp > 0) {
                                    read += tmp;
                                } else {
                                    toRead = 0;
                                }
                                buffer.flip();
                                toRead -= buffer.limit();
                                if (toRead > 0) {
                                    try { command.partial(buffer); } catch (Exception ee) { }
                                } else {
                                    try { command.reply(buffer); } catch (Exception ee) { }
                                }
                                buffer.clear();
                            } while (toRead > 0);
                        }
                    } catch (Exception e) {
                        safeClose(fin);
                        return null;
                    }
                    return fin;
                }) == null && !flag.found) {
                    try { command.notFound(); } catch (Exception ee) { }
                }
            }
        });
    }

    public void handle(SchedulableCreateOrReplaceCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            try (var lock = uFS.readLock()) {                
                if (uFS.filemap.compute(path, (p, fout) -> {
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
                        fout.truncate(0).write(bytes);
                        if (command.requireSync()) {
                            fout.force(true);
                        }
                        try { command.reply(true); } catch (Exception ee) { }
                    } catch (Exception e) {
                        try { command.reply(false); } catch (Exception ee) { }
                        safeClose(fout);
                        return null;
                    }
                    return fout;
                }) == null) {
                    try { command.reply(false); } catch (Exception e) { }
                }
            }
        });
    }

    public void handle(SchedulableTruncateCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            try (var lock = uFS.readLock()) {
                if (uFS.filemap.compute(path, (p, fout) -> {
                    try {
                        if (fout == null) {
                            fout = FileChannel.open(path,  StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                        } else {
                            fout.truncate(command.getLength());
                        }
                    } catch (IOException e) {
                        try { command.reply(false); } catch (Exception ee) { }
                        safeClose(fout);
                        return null;
                    }
                    try { command.reply(true); } catch (Exception ee) { }
                    return fout;
                }) == null) {
                    try { command.reply(false); } catch (Exception e) { }
                }
            }
        });
    }

    public void handle(SchedulableExistsCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            try (var lock = uFS.readLock()) {
                if (uFS.filemap.computeIfPresent(path, (p, fout) -> {
                    try { command.reply(true); } catch (Exception e) { }
                    return fout;
                }) == null) {
                    var exists = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
                    try { command.reply(exists); } catch (Exception e) { }
                }
            }
        });
    }

    public void handle(SchedulableDeleteCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            try (var lock = uFS.readLock()) {
                uFS.filemap.compute(path, (p, fout) -> {
                    try {
                        safeClose(fout);
                        flag.success = Files.deleteIfExists(path);
                    } catch (IOException e) { }
                    return null;
                });
            }
            try { command.reply(flag.success); } catch (Exception e) { }
        });
    }

    public void handle(SchedulableAppendCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            try (var lock = uFS.readLock()) {
                uFS.filemap.computeIfPresent(path, (p, fout) -> {
                    try {
                        var buffer = command.getData();
                        fout.position(fout.size()).write(buffer);
                        if (command.requireSync()) {
                            fout.force(true);
                        }
                        flag.success = true;
                    } catch (IOException e) {
                        safeClose(fout);
                        return null;
                    }
                    return fout;
                });
            }
            try { command.reply(flag.success); } catch (Exception e) { }
        });
    }

    public void handle(SchedulableListCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            Collection<Path> items = new LinkedList<>();
            try (var lock = uFS.readLock()) {
                try (var dirContent = Files.newDirectoryStream(path)) {
                    for (var entry : dirContent) {
                        var r = uFS.userDir.relativize(entry);
                        var e = r.toString();
                        var p = new Path(e, Files.isDirectory(entry));
                        items.add(p);
                    }
                    flag.success = true;
                } catch (Exception e) { }
            }
            if (flag.success) {
                try { command.reply(items); } catch (Exception ee) { }
            } else {
                try { command.notFound(); } catch (Exception ee) { }
            }
        });
    }

    public void handle(SchedulableWriteCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            try (var lock = uFS.readLock()) {
                uFS.filemap.compute(path, (p, fout) -> {
                    try {
                        if (fout == null) {
                            fout = FileChannel.open(p, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                        }
                        var buffer = command.getData();
                        fout.write(buffer, command.getOffset());
                        if (command.requireSync()) {
                            fout.force(true);
                        }
                        flag.success = true;
                    } catch (IOException e) { }
                    return fout;
                });
            }
            try { command.reply(flag.success); } catch (Exception e) { }
        });
    }

    public void handle(SchedulableCreateCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            try (var lock = uFS.readLock()) {
                uFS.filemap.computeIfAbsent(path, (p) -> {
                    FileChannel fout = null;
                    try {
                        fout = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.SPARSE);
                    } catch (IOException e) {
                        return null;
                    }
                    try {
                        var buffer = command.getData();
                        fout.write(buffer);
                        if (command.requireSync()) {
                            fout.force(true);
                        }
                        flag.success = true;
                    } catch (Exception e) { }
                    return fout;
                });
            }
            try { command.reply(flag.success); } catch (Exception e) { }
        });
    }

    public void handle(SchedulableCopyCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var srcPath = java.nio.file.Path.of(uFS.userDir.toString(), command.getSource().getPath());
        var dsrPath = java.nio.file.Path.of(uFS.userDir.toString(), command.getDestination().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            try (var lock = uFS.readLock()) {
                uFS.filemap.computeIfAbsent(dsrPath, (dst) -> {
                    uFS.filemap.compute(srcPath, (src, fout) -> {
                        safeClose(fout);
                        try {
                            Files.copy(srcPath, dsrPath);
                            flag.success = true;
                        } catch (IOException e) { }
                        return null;
                    });
                    return null;
                });
            }
            try { command.reply(flag.success); } catch (Exception ee) { }
        });
    }

    public void handle(SchedulableMoveCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var srcPath = java.nio.file.Path.of(uFS.userDir.toString(), command.getSource().getPath());
        var dstPath = java.nio.file.Path.of(uFS.userDir.toString(), command.getDestination().getPath());
        pool.submit(() -> {
            try (var lock = uFS.writeLock()) {
                if (Files.isDirectory(dstPath)) {
                    try {
                        Files.move(srcPath, dstPath);
                        uFS.filemap.replaceAll((p, fc) -> {
                            if (p.startsWith(dstPath)) {
                                safeClose(fc);
                                return null;
                            } else {
                                return fc;
                            }
                        });
                        try { command.reply(true); } catch (Exception ee) { }
                    } catch (IOException e) {
                        try { command.reply(false); } catch (Exception ee) { }
                    }
                } else {
                    var flag = new Object(){
                        public boolean success = false;
                    };
                    uFS.filemap.computeIfAbsent(dstPath, (dst) -> {
                        uFS.filemap.compute(srcPath, (src, fout) -> {
                            if (fout != null) {
                                try { fout.close(); } catch (IOException ee) { }
                            }
                            try {
                                Files.move(srcPath, dstPath);
                                flag.success = true;
                            } catch (IOException e) { }
                            return null;
                        });
                        return null;
                    });
                    try { command.reply(flag.success); } catch (Exception ee) { }
                }
            }
        });
    }

    public void handle(SchedulableMkdirCommand command) throws ApplicationException {
        var user = command.getUser();
        var uFS = getUserFS(user);
        var path = java.nio.file.Path.of(uFS.userDir.toString(), command.getPath().getPath());
        pool.submit(() -> {
            var flag = new Object(){
                public boolean success = false;
            };
            try (var lock = uFS.readLock()) {
                Files.createDirectory(path);
                flag.success = true;
            } catch (IOException e) { }
            try { command.reply(flag.success); } catch (Exception ee) { }
        });
    }


    private boolean started;
    @Override
    public void scheduleExecution(SchedulableCommand command) throws Exception {
        command.submit(this);
    }

    @Override
    public void start() throws Exception {
        if (this.started) {
            throw new Exception("Executor already started");
        }
        this.started = true;
        this.pool = Executors.newFixedThreadPool(WORKER_THREAD_COUNT);
        this.timedPoll = Executors.newScheduledThreadPool(1);
        this.timedPoll.scheduleWithFixedDelay(() -> {
            var now = Instant.now();
            var oldLimit = now.minusMillis(this.cleanup_delay_ms*this.repeat_after);
            // handle share files
            closeIfOld(defaultFS, oldLimit);
            // handle personal files
            this.userFS.replaceAll((path, userFS) -> {
                closeIfOld(userFS, oldLimit);
                return userFS;
            });
        }, 0, this.cleanup_delay_ms, TimeUnit.MILLISECONDS);
    }
    
    private static void closeIfOld(UserFS userFS, Instant oldLimit) {        
        if (userFS != null) {
            try (var lock = userFS.writeLock()) {
                if (userFS.lastAccess.isBefore(oldLimit)) {
                    closeAllFileChannels(userFS);
                }
            }
        }
    }

    private static void safeClose(FileChannel file) {
        if (file != null) {
            try { file.close(); } catch (IOException e) { }
        }
    }

    private static void closeAllFileChannels(UserFS userFS) {
        if (userFS != null) {
            try (var lock = userFS.writeLock()) {
                userFS.filemap.replaceAll((path, file) -> {
                    safeClose(file);
                    return null;
                });
            }
        }
    }

    private void closeAllFileChannels() {
        closeAllFileChannels(defaultFS);
        this.userFS.replaceAll((path, userFS) -> {
            closeAllFileChannels(userFS);
            return null;
        });
    }

    @Override
    public void stop() throws Exception {
        this.pool.shutdown(); this.pool = null;
        this.timedPoll.shutdown(); this.timedPoll = null;
        closeAllFileChannels();
        if (!this.started) {
            throw new Exception("Executor was not previously started started");
        }
        this.started = false;
    }
}
