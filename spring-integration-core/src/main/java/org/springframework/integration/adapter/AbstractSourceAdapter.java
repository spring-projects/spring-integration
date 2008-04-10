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

package org.springframework.integration.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A base class providing common behavior for source adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractSourceAdapter<T> implements SourceAdapter, InitializingBean {

	protected Log logger = LogFactory.getLog(this.getClass());

	private MessageChannel channel;

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

	public final void afterPropertiesSet() {
		if (this.channel == null) {
			throw new ConfigurationException("'channel' is required");
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

	protected boolean sendToChannel(Message<T> message) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		if (message == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("adapter attempted to send a null object");
			}
			return false;
		}
		if (this.sendTimeout < 0) {
			return this.channel.send(message);
		}
		return this.channel.send(message, this.sendTimeout);
	}

}
