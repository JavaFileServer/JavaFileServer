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
        FileManagerFactory.Help("\t");
        System.out.println();
        RequestHandlerFactory.Help("\t");
        System.out.println();
        System.exit(0);
    }

    public static void main( String[] args ) throws Exception
    {
        try {            
            if (args.length > 0 && args[0].equals("--help")) {
                Help();
            }
            BufferManager.parseArgs(args);
            var executor = FileManagerFactory.getExecutor(args);
            var handler = RequestHandlerFactory.getRequestHandler(executor, args);
            var controller = ControllerFactory.getController();
            executor.start();
            handler.start();
            controller.run(executor, handler);
            handler.stop();
            executor.stop();
        } catch (Exception e) {
            System.err.println("SERVER CRASHED!");
            System.err.println("  message: " + e.getMessage());
            System.err.println("  cause:   " + e.getCause());
            String stackTrace = ""; int i=0;
            for (var st : e.getStackTrace()) {
                stackTrace += "    " + ++i + ") " + st.toString() + "\n";
            }
            System.err.println("  Stacktrace |>");
            System.err.println(stackTrace);
            System.exit(1);
        }
    }
}
