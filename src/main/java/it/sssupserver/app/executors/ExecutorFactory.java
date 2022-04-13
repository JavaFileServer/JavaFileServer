package it.sssupserver.app.executors;

public class ExecutorFactory {
    public static Executor getExecutor()
    {
        //return new DummyStringExecutor();
        return new DummyMapExecutor();
    }
}
