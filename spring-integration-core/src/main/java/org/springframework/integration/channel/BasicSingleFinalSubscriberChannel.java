/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

/**
 * Specialized channel for a single final subscriber. Does not support interceptors or
 * data types.
 * <p>
 * <b>Note: Stopping the subscriber ({@link #unsubscribe(MessageHandler)}) has no effect.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public final class BasicSingleFinalSubscriberChannel implements SubscribableChannel {

	private final Log logger = LogFactory.getLog(BasicSingleFinalSubscriberChannel.class);

	private final MessageHandler handler;

	public BasicSingleFinalSubscriberChannel() {
		throw new IllegalArgumentException("Cannot instantiate a " + this.getClass().getSimpleName()
				+ " without a MessageHandler.");
	}

	public BasicSingleFinalSubscriberChannel(MessageHandler handler) {
		this.handler = handler;
	}

	@Override
	public boolean send(Message<?> message) {
		return this.send(message, 0);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		try {
			this.handler.handleMessage(message);
			return true;
		}
		catch (Exception e) {
			RuntimeException runtimeException = (e instanceof RuntimeException)
					? (RuntimeException) e
					: new MessageDeliveryException(message,
							"Dispatcher failed to deliver Message.", e);
			if (e instanceof MessagingException &&
					((MessagingException) e).getFailedMessage() == null) {
				runtimeException = new MessagingException(message, e);
			}
			throw runtimeException;
		}
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		if (logger.isDebugEnabled()) {
			logger.debug("Cannot unsubscribe from " + this.getClass().getSimpleName() + ".");
		}
		return false;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		if (handler != this.handler && logger.isDebugEnabled()) {
			logger.debug("Cannot subscribe to " + this.getClass().getSimpleName() + ".");
		}
		return false;
	}

}
