/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.test.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
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
    @Override
	@SuppressWarnings("unchecked")
    public void handleMessage(Message<?> message) throws MessagingException {
    	this.lastMessage = message;
        validateResponse((T) (extractPayload()? message.getPayload(): message ));
    }

    /**
     * Implement this method to validate the response (Message or Payload)
     * @param response The response.
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
