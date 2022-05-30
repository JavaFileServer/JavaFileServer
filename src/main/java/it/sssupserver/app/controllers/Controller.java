package it.sssupserver.app.controllers;

import it.sssupserver.app.filemanagers.FileManager;
import it.sssupserver.app.handlers.RequestHandler;

public interface Controller {
    public void run(FileManager executor, RequestHandler requestHandler) throws Exception;
}
