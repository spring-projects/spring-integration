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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.MessageChannel;

/**
 * @author Mark Fisher
 */
public class MapBasedChannelResolverTests {

	@Test
	public void mapContainsChannel() {
		MessageChannel testChannel = new QueueChannel();
		Map<String, MessageChannel> channelMap = new HashMap<String, MessageChannel>();
		channelMap.put("testChannel", testChannel);
		MapBasedChannelResolver resolver = new MapBasedChannelResolver();
		resolver.setChannelMap(channelMap);
		MessageChannel result = resolver.resolveChannelName("testChannel");
		assertNotNull(result);
		assertEquals(testChannel, result);
	}

	@Test
	public void mapDoesNotContainChannel() {
		MessageChannel testChannel = new QueueChannel();
		Map<String, MessageChannel> channelMap = new HashMap<String, MessageChannel>();
		channelMap.put("testChannel", testChannel);
		MapBasedChannelResolver resolver = new MapBasedChannelResolver();
		resolver.setChannelMap(channelMap);
		MessageChannel result = resolver.resolveChannelName("noSuchChannel");
		assertNull(result);
	}

	@Test
	public void emptyMap() {
		Map<String, MessageChannel> channelMap = new HashMap<String, MessageChannel>();
		MapBasedChannelResolver resolver = new MapBasedChannelResolver();
		resolver.setChannelMap(channelMap);
		MessageChannel result = resolver.resolveChannelName("testChannel");
		assertNull(result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMapRejected() {
		MapBasedChannelResolver resolver = new MapBasedChannelResolver();
		resolver.setChannelMap(null);
	}

}
