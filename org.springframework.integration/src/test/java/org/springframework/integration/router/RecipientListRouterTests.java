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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RecipientListRouterTests {

	@Test
	public void resolveWithChannelList() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channel1);
		channels.add(channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(channels);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		Collection<MessageChannel> resolved = router.determineTargetChannels(message);
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(channel1));
		assertTrue(resolved.contains(channel2));
	}

	@Test
	public void routeWithChannelList() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channel1);
		channels.add(channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(channels);
		router.afterPropertiesSet();
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel2.receive(25);
		assertNotNull(result2);
		assertEquals("test", result2.getPayload());
	}

	@Test
	public void routeToSingleChannel() {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("channel");
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(Collections.singletonList((MessageChannel) channel));
		Message<String> message = new StringMessage("test");
		router.onMessage(message);
		Message<?> result1 = channel.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = channel.receive(5);
		assertNull(result2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullChannelListRejected() {
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(null);
		router.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyChannelListRejected() {
		RecipientListRouter router = new RecipientListRouter();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		router.setChannels(channels);
		router.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noChannelListFailsInitialization() {
		RecipientListRouter router = new RecipientListRouter();
		router.afterPropertiesSet();
	}

}
