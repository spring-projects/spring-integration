/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.util.ErrorHandler;

/**
 * The base class for Message Endpoint implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint implements MessageEndpoint, ChannelRegistryAware, BeanNameAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private MessageSource<?> source;

	private MessageTarget target;

	private volatile Schedule schedule;

	private volatile ErrorHandler errorHandler;

	private volatile ChannelRegistry channelRegistry;

	private final MessageExchangeTemplate messageExchangeTemplate = new MessageExchangeTemplate();


	/**
	 * Return the name of this endpoint.
	 */
	public String getName() {
		return this.name;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	public MessageSource<?> getSource() {
		return this.source;
	}

	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public MessageTarget getTarget() {
		return this.target;
	}

	public void setTarget(MessageTarget target) {
		this.target = target;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected MessageExchangeTemplate getMessageExchangeTemplate() {
		return this.messageExchangeTemplate;
	}

	/**
	 * Provide an error handler for any Exceptions that occur
	 * upon invocation of this endpoint. If none is provided,
	 * the Exception messages will be logged (at warn level),
	 * and the Exception rethrown.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public final boolean send(Message<?> message) {
		if (message == null || message.getPayload() == null) {
			throw new IllegalArgumentException("Message and its payload must not be null");
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("endpoint '" + this + "' processing message: " + message);
		}
		try {
			return this.sendInternal(message);
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				this.handleException((MessagingException) e);
			}
			else {
				this.handleException(new MessageHandlingException(message,
						"failure occurred in endpoint '" + this.toString() + "'", e));
			}
			return false;
		}
	}

	protected abstract boolean sendInternal(Message<?> message);

	private void handleException(MessagingException exception) {
		if (this.errorHandler == null) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("exception occurred in endpoint '" + this.name + "'", exception);
			}
			throw exception;
		}
		this.errorHandler.handle(exception);
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

}
