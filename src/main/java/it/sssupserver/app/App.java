package it.sssupserver.app;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.base.BufferManager;
import it.sssupserver.app.controllers.ControllerFactory;
import it.sssupserver.app.filemanagers.*;

/**
 * Hello world!
 */
public class App
{
    static void Help() {
        System.err.println("Usage:");
        BufferManager.Help("\t");
        System.out.println();
        ExecutorFactory.Help("\t");
        System.out.println();
        RequestHandlerFactory.Help("\t");
        System.out.println();
        System.exit(0);
    }

    public static void main( String[] args ) throws Exception
    {
        if (args.length > 0 && args[0].equals("--help")) {
            Help();
        }
        BufferManager.parseArgs(args);
        var executor = ExecutorFactory.getExecutor(args);
        var handler = RequestHandlerFactory.getRequestHandler(executor, args);
        var controller = ControllerFactory.getController();
        executor.start();
        handler.start();
        controller.run(executor, handler);
        handler.stop();
        executor.stop();
    }
}
