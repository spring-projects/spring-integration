/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.core.MessageChannel;
import org.springframework.util.Assert;

/**
 * {@link ChannelResolver} implementation that resolves {@link MessageChannel}
 * instances by matching the channel name against keys within a Map.
 * 
 * @author Mark Fisher
 */
public class MapBasedChannelResolver implements ChannelResolver {

	private volatile Map<String, MessageChannel> channelMap = new HashMap<String, MessageChannel>();

	/**
	 * Empty constructor for use when providing the channel map via
	 * {@link #setChannelMap(Map)}.
	 */
	public MapBasedChannelResolver() {
	}

	/**
	 * Create a {@link ChannelResolver} that uses the provided Map.
	 * Each String key will resolve to the associated channel value.
	 */
	public MapBasedChannelResolver(Map<String, MessageChannel> channelMap) {
		this.setChannelMap(channelMap);
	}

	/**
	 * Provide a map of channels to be used by this resolver.
	 * Each String key will resolve to the associated channel value.
	 */
	public void setChannelMap(Map<String, MessageChannel> channelMap) {
		Assert.notNull(channelMap, "channelMap must not be null");
		this.channelMap = channelMap;
	}

	public MessageChannel resolveChannelName(String channelName) {
		return this.channelMap.get(channelName);
	}

}
