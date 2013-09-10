package org.springframework.integration.test.support;

import org.springframework.messaging.Message;
/**
 * Validate a message. Create an anonymous instance or subclass to 
 * implement the validateMessage() method
 * @author David Turanski
 *
 */
public abstract class MessageValidator extends AbstractResponseValidator<Message<?>> {
    protected final boolean extractPayload(){
        return false;
    }
    
    protected final void validateResponse(Message<?> response){
        validateMessage((Message<?>) response);
    }
    /**
     * Implement this method to validate the message
     * @param message
     */
    protected abstract void validateMessage(Message<?> message);
    
}
