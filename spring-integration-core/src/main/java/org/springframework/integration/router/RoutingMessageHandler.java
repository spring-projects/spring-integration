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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class RoutingMessageHandler implements MessageHandler, ChannelRegistryAware, InitializingBean {

	private ChannelResolver channelResolver;

	private ChannelNameResolver channelNameResolver;

	private ChannelRegistry channelRegistry;

	private long timeout = -1;


	/**
	 * Set the timeout for sending a message to the resolved channel.
	 * Default is no timeout, meaning the send will block indefinitely.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setChannelNameResolver(ChannelNameResolver channelNameResolver) {
		this.channelNameResolver = channelNameResolver;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	public void afterPropertiesSet() {
		if(!(this.channelResolver != null ^ this.channelNameResolver != null)) {
			throw new MessagingConfigurationException("exactly one of 'channelResolver' or 'channelNameResolver' must be provided");
		}
		if (this.channelNameResolver != null && this.channelRegistry == null) {
			throw new MessagingConfigurationException("'channelRegistry' is required when resolving by channel name");
		}
	}

	public Message<?> handle(Message<?> message) {
		MessageChannel channel = this.resolveChannel(message);
		if (channel == null) {
			throw new MessageHandlingException("failed to resolve channel");
		}
		boolean sent = false;
		if (timeout < 0) {
			sent = channel.send(message);
		}
		else {
			sent = channel.send(message, timeout);
		}
		if (!sent) {
			throw new MessageHandlingException(
					"failed to send message to channel '" + channel.getName() + "'");
		}
		return null;
	}

	private MessageChannel resolveChannel(Message<?> message) {
		if (this.channelResolver != null) {
			return this.channelResolver.resolve(message);
		}
		else if (this.channelNameResolver != null && this.channelRegistry != null) {
			String channelName = this.channelNameResolver.resolve(message);
			return this.channelRegistry.lookupChannel(channelName);
		}
		throw new MessagingConfigurationException("router configuration requires either " +
				"a 'channelResolver' or both 'channelNameResolver' and 'channelRegistry'");
	}

}
