package org.springframework.integration.test.support;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
/**
 * The base class for response validators used for {@link RequestResponseScenario}s
 * @author David Turanski
 *
 */
public abstract class AbstractResponseValidator<T> implements MessageHandler {
	
	private Message<?> lastMessage;
    /**
     * handle the message 
     */
    @SuppressWarnings("unchecked")
	//@Override
    public void handleMessage(Message<?> message) throws MessagingException {
    	this.lastMessage = message;
        validateResponse((T) (extractPayload()? message.getPayload(): message ));
    }
    /**
     * Implement this method to validate the response (Message or Payload)
     * @param response
     */
    protected abstract void validateResponse(T response);
    /**
     * If true will extract the payload as the parameter for validateResponse()
     * @return true to extract the payload; false to process the message.
     */
    protected abstract boolean extractPayload();
	/**
	 * @return the lastMessage
	 */
	public Message<?> getLastMessage() {
		return lastMessage;
	}

}
