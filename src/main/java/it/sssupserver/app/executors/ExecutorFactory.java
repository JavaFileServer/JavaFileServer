package it.sssupserver.app.executors;

import it.sssupserver.app.executors.samples.*;

public class ExecutorFactory {
    public static Executor getExecutor() throws Exception
    {
        //return new DummyStringExecutor();
        //return new DummyMapExecutor();
        return new FlatTmpExecutor();
    }
}
