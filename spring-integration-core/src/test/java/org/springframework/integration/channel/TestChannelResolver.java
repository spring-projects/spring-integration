/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class TestChannelResolver implements DestinationResolver<MessageChannel> {

	private volatile Map<String, MessageChannel> channels = new ConcurrentHashMap<String, MessageChannel>();

	public MessageChannel resolveDestination(String channelName) {
		return this.channels.get(channelName);
	}

	@Autowired
	public void setChannels(Map<String, MessageChannel> channels) {
		this.channels = channels;
	}

	public void addChannel(String name, MessageChannel channel) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(channel, "channel must not be null");
		this.channels.put(name, channel);
	}

}
