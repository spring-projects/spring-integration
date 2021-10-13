/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

/**
 * Specialized {@link SubscribableChannel} for a single final subscriber set up during bean instantiation (unlike
 * other {@link SubscribableChannel}s where the {@link MessageHandler} is subscribed when the endpoint
 * is started). This channel does not support interceptors or data types.
 * <p>
 * <b>Note: Stopping ({@link #unsubscribe(MessageHandler)}) the subscribed ({@link MessageHandler}) has no effect.</b>
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public final class FixedSubscriberChannel implements SubscribableChannel, BeanNameAware, NamedComponent {

	private static final Log LOGGER = LogFactory.getLog(FixedSubscriberChannel.class);

	private final MessageHandler handler;

	private String beanName;

	public FixedSubscriberChannel() {
		throw new IllegalArgumentException("Cannot instantiate a " + this.getClass().getSimpleName()
				+ " without a MessageHandler.");
	}

	public FixedSubscriberChannel(MessageHandler handler) {
		this.handler = handler;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public boolean send(Message<?> message) {
		return send(message, 0);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		try {
			this.handler.handleMessage(message);
			return true;
		}
		catch (MessagingException ex) {
			if (ex.getFailedMessage() == null) {
				throw new MessagingException(message, "Failed to handle Message", ex);
			}
			else {
				throw ex;
			}
		}
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		if (!this.handler.equals(handler) && LOGGER.isDebugEnabled()) {
			LOGGER.debug(getComponentName() + ": cannot be subscribed to (it has a fixed single subscriber).");
		}
		return false;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(getComponentName() + ": cannot be unsubscribed from (it has a fixed single subscriber).");
		}
		return false;
	}

	@Override
	public String getComponentType() {
		return "fixed-subscriber-channel";
	}

	@Override
	public String getComponentName() {
		if (this.beanName != null) {
			return this.beanName;
		}
		else {
			return "Unnamed fixed subscriber channel";
		}
	}

}
