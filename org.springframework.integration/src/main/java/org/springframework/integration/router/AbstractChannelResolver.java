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

import java.util.Collection;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;

/**
 * @author Mark Fisher
 */
public abstract class AbstractChannelResolver implements ChannelResolver, ChannelRegistryAware {

	private volatile ChannelRegistry channelRegistry;


	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected MessageChannel lookupChannel(String channelName, boolean required) {
		MessageChannel channel = null;
		if (channelName != null) {
			if (this.channelRegistry == null) {
				throw new ConfigurationException("unable to resolve channels, no ChannelRegistry available");
			}
			channel = this.channelRegistry.lookupChannel(channelName);
		}
		if (channel == null && required) {
			throw new MessagingException("unable to resolve channel '" + channelName + "'");
		}
		return channel;
	}

	public abstract Collection<MessageChannel> resolveChannels(Message<?> message);

}
