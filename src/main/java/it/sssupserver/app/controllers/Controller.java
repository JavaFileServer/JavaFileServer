package it.sssupserver.app.controllers;

import it.sssupserver.app.executors.Executor;
import it.sssupserver.app.handlers.RequestHandler;

public interface Controller {
    public void run(Executor executor, RequestHandler requestHandler);
}
