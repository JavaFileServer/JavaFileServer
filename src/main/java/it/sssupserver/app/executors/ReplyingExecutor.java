package it.sssupserver.app.executors;

import it.sssupserver.app.commands.*;
import it.sssupserver.app.repliers.*;

/**
 * Using Replier(s) instead of
 * Schedulable commands require
 * much more attention to details.
 * By not requiring by default
 * for executors to supports Replyer
 * code became much smaller.
 */
public interface ReplyingExecutor extends Executor {
    /**
     * Execute a single command.
     * Blocking.
     */
    public void execute(Command command, Replier replier) throws Exception;
    /**
     * Schedule command for execution.
     * May be asynchronous and non-blocking.
     */
    public void scheduleExecution(Command command, Replier replier) throws Exception;
}
