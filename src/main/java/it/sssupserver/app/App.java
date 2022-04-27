package it.sssupserver.app;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.controllers.ControllerFactory;
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
        var controller = ControllerFactory.getController();
        executor.start();
        System.out.println( "Wait for message..." );
        handler.start();
        //var cmd = handler.receiveCommand();
        //System.out.println("Cmd: " + cmd);
        controller.run(executor, handler);
        handler.stop();
        //handler.receiveAndExecuteCommand();
        executor.stop();
        System.out.println("MAIN terminated!");
    }
}
