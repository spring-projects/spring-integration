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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class PayloadTypeRouterTests {

	@Test
	public void resolveByPayloadType() {
		QueueChannel stringChannel = new QueueChannel();
		QueueChannel integerChannel = new QueueChannel();
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(String.class, stringChannel);
		payloadTypeChannelMap.put(Integer.class, integerChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		Message<String> message1 = new StringMessage("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		MessageChannel result1 = router.determineTargetChannel(message1);
		MessageChannel result2 = router.determineTargetChannel(message2);
		assertEquals(stringChannel, result1);
		assertEquals(integerChannel, result2);
	}

	@Test
	public void resolveByPayloadTypeWithRouterEndpoint() {
		QueueChannel stringChannel = new QueueChannel();
		QueueChannel integerChannel = new QueueChannel();
		stringChannel.setBeanName("stringChannel");
		integerChannel.setBeanName("integerChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(String.class, stringChannel);
		payloadTypeChannelMap.put(Integer.class, integerChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		Message<String> message1 = new StringMessage("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		router.onMessage(message1);
		router.onMessage(message2);
		Message<?> reply1 = stringChannel.receive(0);
		Message<?> reply2 = integerChannel.receive(0);
		assertEquals("test", reply1.getPayload());
		assertEquals(123, reply2.getPayload());
	}

	@Test
	public void routingToDefaultChannelWhenNoTypeMatches() {
		QueueChannel stringChannel = new QueueChannel();
		stringChannel.setBeanName("stringChannel");
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(String.class, stringChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message1 = new StringMessage("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		router.onMessage(message1);
		router.onMessage(message2);
		Message<?> result1 = stringChannel.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = defaultChannel.receive(25);
		assertNotNull(result2);
		assertEquals(123, result2.getPayload());
	}

}
