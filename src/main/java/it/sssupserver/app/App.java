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
        System.out.println( "Wait for message..." );
        //var cmd = handler.receiveCommand();
        //System.out.println("Cmd: " + cmd);
        handler.receiveAndExecuteCommand();
        System.out.println("MAIN terminated!");
    }
}
