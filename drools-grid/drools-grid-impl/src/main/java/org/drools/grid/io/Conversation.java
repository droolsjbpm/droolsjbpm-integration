package org.drools.grid.io;

public interface Conversation {

    void respond(Object body);
    
    void respondError(Throwable t);

    void sendMessage(Object body,
                     MessageReceiverHandler handler);
    
    void endConversation();

}
