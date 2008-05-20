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
 * A router implementation that resolves the {@link MessageChannel} for messages
 * whose payload is an Exception. The channel resolution is based upon the most
 * specific cause of the error for which a channel-mapping exists.
 * 
 * @author Mark Fisher
 */
public class RootCauseErrorMessageRouter extends SingleChannelRouter {

	private Map<Class<? extends Throwable>, MessageChannel> channelMappings =
			new ConcurrentHashMap<Class<? extends Throwable>, MessageChannel>();

	private MessageChannel defaultChannel;


	public RootCauseErrorMessageRouter() {
		this.setChannelResolver(new RootCauseResolver());
	}


	public void setChannelMappings(Map<Class<? extends Throwable>, MessageChannel> channelMappings) {
		Assert.notNull(channelMappings, "'channelMappings' must not be null");
		this.channelMappings = channelMappings;
	}

	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.defaultChannel = defaultChannel;
	}


	private class RootCauseResolver implements ChannelResolver {

		public MessageChannel resolve(Message<?> message) {
			MessageChannel channel = null;
			Object payload = message.getPayload();
			if (payload != null && (payload instanceof Throwable)) {
				Throwable mostSpecificCause = (Throwable) payload;
				while (mostSpecificCause != null) {
					MessageChannel mappedChannel = channelMappings.get(mostSpecificCause.getClass());
					if (mappedChannel != null) {
						channel = mappedChannel;
					}
					mostSpecificCause = mostSpecificCause.getCause();
				}
			}
			return channel != null ? channel : defaultChannel;
		}
	}

}
