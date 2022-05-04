package it.sssupserver.app.executors;

import java.nio.file.Path;
import java.nio.file.Paths;

import it.sssupserver.app.executors.samples.*;

public class ExecutorFactory {
    public static Executor getExecutor() throws Exception
    {
        //return new DummyStringExecutor();
        //return new DummyMapExecutor();
        //return new FlatTmpExecutor();
        //return new UserTreeExecutor();
        var datadir = Paths.get("").resolve("file-server");
        return new UserTreeExecutor(datadir);
    }
}
