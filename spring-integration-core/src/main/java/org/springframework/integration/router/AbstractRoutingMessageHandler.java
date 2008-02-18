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

package org.springframework.integration.router;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * Base class for message router implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRoutingMessageHandler implements MessageHandler, ChannelRegistryAware, InitializingBean {

	protected Log logger = LogFactory.getLog(this.getClass());

	private ChannelRegistry channelRegistry;

	private boolean resolutionRequired = false;

	private long timeout = -1;


	/**
	 * Set whether this router should always be required to resolve at least one
	 * channel. The default is 'false'. To trigger an exception whenever the
	 * resolver returns null or an empty channel list, set this value to 'true'.
	 */
	public void setResolutionRequired(boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	/**
	 * Set the timeout for sending a message to the resolved channel. By
	 * default, there is no timeout, meaning the send will block indefinitely.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	public final void afterPropertiesSet() {
		this.validate();
	}

	public final Message<?> handle(Message<?> message) {
		List<MessageChannel> channels = this.resolveChannels(message);
		if (channels == null || channels.size() == 0) {
			String errorMessage = "failed to resolve any channel for message";
			if (this.resolutionRequired) {
				throw new MessageHandlingException(errorMessage);
			}
			if (logger.isWarnEnabled()) {
				logger.warn(errorMessage);
			}
			return null;
		}
		for (MessageChannel channel : channels) {
			this.sendMesage(message, channel);
		}
		return null;
	}

	private void sendMesage(Message<?> message, MessageChannel channel) {
		boolean sent = false;
		if (timeout < 0) {
			sent = channel.send(message);
		}
		else {
			sent = channel.send(message, timeout);
		}
		if (!sent) {
			throw new MessageHandlingException("failed to send message to channel '" + channel.getName() + "'");
		}
	}

	protected abstract void validate() throws MessagingConfigurationException;

	protected abstract List<MessageChannel> resolveChannels(Message<?> message);

}
