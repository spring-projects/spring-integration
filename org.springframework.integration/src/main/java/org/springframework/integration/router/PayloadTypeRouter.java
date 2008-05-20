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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A router implementation that resolves the {@link MessageChannel} based on the
 * {@link Message Message's} payload type.
 * 
 * @author Mark Fisher
 */
public class PayloadTypeRouter extends SingleChannelRouter {

	private Map<Class<?>, MessageChannel> channelMappings = new ConcurrentHashMap<Class<?>, MessageChannel>();

	private MessageChannel defaultChannel;


	public PayloadTypeRouter() {
		this.setChannelResolver(new PayloadTypeChannelResolver());
	}


	public void setChannelMappings(Map<Class<?>, MessageChannel> channelMappings) {
		Assert.notNull(channelMappings, "'channelMappings' must not be null");
		this.channelMappings = channelMappings;
	}

	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.defaultChannel = defaultChannel;
	}


	private class PayloadTypeChannelResolver implements ChannelResolver {

		public MessageChannel resolve(Message<?> message) {
			MessageChannel channel = channelMappings.get(message.getPayload().getClass());
			return channel != null ? channel : defaultChannel;
		}
	}

}
