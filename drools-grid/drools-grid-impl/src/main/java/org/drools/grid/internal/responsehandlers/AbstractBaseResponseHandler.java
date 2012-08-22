package org.drools.grid.internal.responsehandlers;

import java.lang.reflect.Constructor;
import org.drools.grid.io.Conversation;

import org.drools.grid.io.MessageReceiverHandler;
import org.drools.grid.io.impl.ExceptionMessage;

/**
 * Abstract base class for client ResponseHandlers. Provides synchonized access to <field>done</field> which represents
 * if the response is completed. Also has an <field>error</field> which will be set when there is a problem with
 * a response. Users of this class should check to see if the response completed successfully, via
 * the <method>isDone</method> and the <method>hasError</method>.
 * <p/>
 * Please note that the <field>error</field> is actually the Exception that occured on the server while
 * processing the request.
 */
public abstract class AbstractBaseResponseHandler
    implements
    MessageReceiverHandler {
    private volatile boolean done;
    private Throwable error;

    public void exceptionReceived(Conversation conversation, ExceptionMessage msg) {
        this.setError(msg.getBody());
    }

    
    
    public synchronized boolean hasError() {
        return this.error != null;
    }

    public synchronized Throwable getError() {
        return this.error;
    }

    public synchronized void setError(Throwable error) {
        this.error = error;
        notifyAll();
    }

    public synchronized boolean isDone() {
        return this.done;
    }

    protected synchronized void setDone(boolean done) {
        this.done = done;
        notifyAll();
    }

    /**
     * This method will take the specified serverSideException, and create a new one for the client based
     * on the serverSideException. This is done so a proper stack trace can be made for the client, as opposed
     * to seeing the server side stack.
     *
     * @param serverSideException exception used to create client side exception
     * @return client side exception
     */
    /*
    protected static RuntimeException createSideException(RuntimeException serverSideException) {
        RuntimeException clientSideException;

        try {
            Constructor< ? extends RuntimeException> constructor = serverSideException.getClass().getConstructor( String.class );

            clientSideException = constructor.newInstance( "Server-side Exception: " + serverSideException.getMessage() );
        } catch ( Exception e ) {
            // this should never happen - if it does, it is a programming error
            throw new RuntimeException( "Could not create client side exception",
                                        e );
        }

        return clientSideException;
    }
    */
}
