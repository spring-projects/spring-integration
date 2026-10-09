/*
 * Copyright 2011-present the original author or authors.
 */

package org.springframework.integration.test.support;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * The base class for response validators used for {@link RequestResponseScenario}s
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 */
public abstract class AbstractResponseValidator<T> implements MessageHandler {

	private @Nullable Message<?> lastMessage;

	/**
	 * handle the message
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void handleMessage(Message<?> message) throws MessagingException {
		this.lastMessage = message;
		validateResponse((T) (extractPayload() ? message.getPayload() : message));
	}

	/**
	 * @return the lastMessage
	 */
	public @Nullable Message<?> getLastMessage() {
		return this.lastMessage;
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

}
