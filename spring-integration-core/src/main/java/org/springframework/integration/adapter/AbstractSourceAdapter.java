/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * A base class providing common behavior for source adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractSourceAdapter<T> implements SourceAdapter, InitializingBean {

	protected Log logger = LogFactory.getLog(this.getClass());

	private MessageChannel channel;

	private MessageMapper<?,T> mapper = new SimplePayloadMessageMapper<T>();

	private ConsumerPolicy consumerPolicy;

	private long sendTimeout = -1;

	private volatile boolean initialized = false;


	public void setChannel(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	protected MessageChannel getChannel() {
		return this.channel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setMessageMapper(MessageMapper<?,T> mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected MessageMapper<?,T> getMessageMapper() {
		return this.mapper;
	}

	public void setConsumerPolicy(ConsumerPolicy consumerPolicy) {
		Assert.notNull(consumerPolicy, "'consumerPolicy' must not be null");
		this.consumerPolicy = consumerPolicy;
	}

	public ConsumerPolicy getConsumerPolicy() {
		return this.consumerPolicy;
	}

	public final void afterPropertiesSet() {
		if (this.channel == null) {
			throw new MessagingConfigurationException("'channel' is required");
		}
		this.initialize();
		this.initialized = true;
	}

	protected boolean isInitialized() {
		return this.initialized;
	}

	/**
	 * Subclasses may implement this to take advantage of the initialization callback.
	 */
	protected void initialize() {		
	}

	protected boolean sendToChannel(T object) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		if (object == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("adapter attempted to send a null object");
			}
			return false;
		}
		Message<?> message = null;
		if (object instanceof Message<?>) {
			message = (Message<?>) object;
		}
		else {
			message = this.mapper.toMessage(object);
		}
		if (message == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("unable to create Message from source object: " + object);
			}
			return false;
		}
		if (this.sendTimeout < 0) {
			return this.channel.send(message);
		}
		return this.channel.send(message, this.sendTimeout);
	}

}
