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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.support.context.NamedComponent;
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
public final class BasicSingleFinalSubscriberChannel implements SubscribableChannel, BeanNameAware, NamedComponent {

	private final Log logger = LogFactory.getLog(BasicSingleFinalSubscriberChannel.class);

	private final MessageHandler handler;

	private volatile String beanName;

	public BasicSingleFinalSubscriberChannel() {
		throw new IllegalArgumentException("Cannot instantiate a " + this.getClass().getSimpleName()
				+ " without a MessageHandler.");
	}

	public BasicSingleFinalSubscriberChannel(MessageHandler handler) {
		this.handler = handler;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
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
							this.getComponentName() + " failed to deliver Message.", e);
			if (e instanceof MessagingException &&
					((MessagingException) e).getFailedMessage() == null) {
				runtimeException = new MessagingException(message, e);
			}
			throw runtimeException;
		}
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		if (handler != this.handler && logger.isDebugEnabled()) {
			logger.debug(this.getComponentName() + ": cannot be subscribed to.");
		}
		return false;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		if (logger.isDebugEnabled()) {
			logger.debug(this.getComponentName() + ": cannot be unsubscribed from.");
		}
		return false;
	}

	@Override
	public String getComponentType() {
		return "Final Channel";
	}

	@Override
	public String getComponentName() {
		if (this.beanName != null) {
			return "Final '" + this.beanName + "' channel";
		}
		else {
			return "Unnamed final channel";
		}
	}

}
