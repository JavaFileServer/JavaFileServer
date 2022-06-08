package it.sssupserver.app.controllers.samples;

import java.util.Scanner;

import it.sssupserver.app.controllers.Controller;
import it.sssupserver.app.filemanagers.FileManager;
import it.sssupserver.app.handlers.RequestHandler;

/**
 * Simple controller that require the user to insert the command "stop" and
 * confirm to exit
 */
public class StoppableController implements Controller {
    @Override
    public void run(FileManager executor, RequestHandler requestHandler) throws Exception {
        var exit = false;
        try(var in = new Scanner(System.in)) {
            do {
                System.out.print("Insert 'stop' to exit> ");
                var line = in.nextLine();
                if (line.equals("stop")) {
                    System.out.print("Confirm [y/N]? ");
                    var confirm = in.nextLine();
                    if (confirm.equals("y")) {
                        exit = true;
                    }
                }
            } while (!exit);
        }
    }
}
