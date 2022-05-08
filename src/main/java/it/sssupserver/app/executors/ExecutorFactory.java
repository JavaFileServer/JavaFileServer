package it.sssupserver.app.executors;

import java.nio.file.Paths;

import it.sssupserver.app.executors.samples.*;

public class ExecutorFactory {
    // Command line arguments starting with this prefix
    // are intended as directed to an executor
    private static final String argsPrefix = "--X";

    public static Executor getExecutor() throws Exception {
        return getExecutor(null);
    }

    // initialize
    public static Executor getExecutor(String[] args) throws Exception {
        //return new DummyStringExecutor();
        //return new DummyMapExecutor();
        //return new FlatTmpExecutor();
        //return new UserTreeExecutor();
        var datadir = Paths.get("").resolve("file-server");
        return new UserTreeExecutor(datadir);
    }
}
