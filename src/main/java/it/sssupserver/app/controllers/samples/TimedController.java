package it.sssupserver.app.controllers.samples;

import it.sssupserver.app.controllers.Controller;
import it.sssupserver.app.filemanagers.FileManager;
import it.sssupserver.app.handlers.RequestHandler;

public class TimedController implements Controller {
    private long millis = 5*60*1000*60;

    public TimedController() {}

    public TimedController(long millis) {
        this.millis = millis;
    }

    @Override
    public void run(FileManager executor, RequestHandler requestHandler) throws InterruptedException {
        Thread.sleep(this.millis);
    }
}
