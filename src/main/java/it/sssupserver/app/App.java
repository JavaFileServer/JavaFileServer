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
        var executor = ExecutorFactory.getExecutor(args);
        var handler = RequestHandlerFactory.getRequestHandler(executor, args);
        var controller = ControllerFactory.getController();
        executor.start();
        handler.start();
        //var cmd = handler.receiveCommand();
        //System.out.println("Cmd: " + cmd);
        controller.run(executor, handler);
        handler.stop();
        //handler.receiveAndExecuteCommand();
        executor.stop();
    }
}
