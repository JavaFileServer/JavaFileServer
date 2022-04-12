package it.sssupserver.app.repliers;

/**
 * A replier is a object supplied to an
 * Executor used to return the answer to
 * the client.
 *
 * A replier coud be used once ore more
 * to response to a request.
 */
public interface Replier {
    /**
     * Send all the content of the given
     * array as it was the whole file
     * content
     */
    public void replyRead(byte[] data) throws Exception;
}
