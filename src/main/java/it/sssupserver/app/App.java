package it.sssupserver.app;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.executors.*;

/**
 * Hello world!
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Hello World!" );
        System.out.println( "Test handler" );
        var executor = ExecutorFactory.getExecutor();
        var handler = RequestHandlerFactory.getRequestHandler(executor);
        executor.start();
        System.out.println( "Wait for message..." );
        handler.start();
        //var cmd = handler.receiveCommand();
        //System.out.println("Cmd: " + cmd);
        Thread.sleep(1000);
        handler.stop();
        //handler.receiveAndExecuteCommand();
        executor.stop();
        System.out.println("MAIN terminated!");
    }
}
