package it.sssupserver.app.controllers;

import it.sssupserver.app.controllers.samples.*;

/**
 * ControllerFactory
 */
public class ControllerFactory {
    public static Controller getController() {
        //return new TimedController();
        return new StoppableController();
    }
}