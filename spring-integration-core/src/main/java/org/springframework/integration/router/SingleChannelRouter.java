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

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;

/**
 * A router implementation for sending to at most one {@link MessageChannel}.
 * Requires either a {@link ChannelResolver} or {@link ChannelNameResolver}
 * strategy instance. In the case of the latter, the
 * {@link org.springframework.integration.channel.ChannelRegistry} reference
 * must also be provided. For convenience, the superclass does implement
 * {@link org.springframework.integration.channel.ChannelRegistryAware}.
 * 
 * @author Mark Fisher
 */
public class SingleChannelRouter extends AbstractRoutingMessageHandler {

	private ChannelResolver channelResolver;

	private ChannelNameResolver channelNameResolver;


	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setChannelNameResolver(ChannelNameResolver channelNameResolver) {
		this.channelNameResolver = channelNameResolver;
	}

	@Override
	public void validate() {
		if (!(this.channelResolver != null ^ this.channelNameResolver != null)) {
			throw new MessagingConfigurationException(
					"exactly one of 'channelResolver' or 'channelNameResolver' must be provided");
		}
		if (this.channelNameResolver != null && this.getChannelRegistry() == null) {
			throw new MessagingConfigurationException("'channelRegistry' is required when resolving by channel name");
		}
	}

	@Override
	public List<MessageChannel> resolveChannels(Message<?> message) {
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		MessageChannel channel = this.resolveChannel(message);
		if (channel != null) {
			channels.add(channel);
		}
		return channels;
	}

	private MessageChannel resolveChannel(Message<?> message) {
		if (this.channelResolver != null) {
			return this.channelResolver.resolve(message);
		}
		if (this.channelNameResolver == null || this.getChannelRegistry() == null) {
			throw new MessagingConfigurationException("router configuration requires either "
					+ "a 'channelResolver' or both 'channelNameResolver' and 'channelRegistry'");
		}
		String channelName = this.channelNameResolver.resolve(message);
		return this.getChannelRegistry().lookupChannel(channelName);
	}

}
