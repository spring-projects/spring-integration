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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A base class for {@link ChannelResolver} implementations that return only
 * the channel name(s) rather than {@link MessageChannel} instances.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractChannelNameResolver implements ChannelResolver, ChannelRegistryAware {

	private ChannelRegistry channelRegistry;


	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	public final Collection<MessageChannel> resolveChannels(Message<?> message) {
		Collection<MessageChannel> channels = new ArrayList<MessageChannel>();
		String[] channelNames = this.resolveChannelNames(message);
		if (channelNames == null) {
			return null;
		}
		for (String channelName : channelNames) {
			if (channelName != null) {
				Assert.state(this.channelRegistry != null,
						"unable to resolve channels, no ChannelRegistry available");
				MessageChannel channel = this.channelRegistry.lookupChannel(channelName);
				if (channel == null) {
					throw new MessagingException(message,
							"unable to resolve chnanel '" + channelName + "'");
				}
				channels.add(channel);
			}
		}
		return channels;
	}

	/**
	 * Subclasses must implement this method to return the channel name(s).
	 */
	protected abstract String[] resolveChannelNames(Message<?> message);

}
