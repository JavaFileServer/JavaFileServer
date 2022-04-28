package it.sssupserver.app.executors.samples;

import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.executors.Executor;

public class UserTreeExecutor implements Executor {
    private String prefix = "JAVA-FILE-SERVER-";
    private java.nio.file.Path baseDir;
    private ExecutorService pool;

    public UserTreeExecutor() throws Exception
    {
        this.baseDir = Files.createTempDirectory(prefix);
        System.out.println("Dase directory: " + this.baseDir);
        this.baseDir.toFile().deleteOnExit();
    }

    private boolean started;
    @Override
    public void scheduleExecution(SchedulableCommand command) throws Exception {
        // TODO Auto-generated method stub

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
