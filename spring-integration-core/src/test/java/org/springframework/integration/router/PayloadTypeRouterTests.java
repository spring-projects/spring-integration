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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class PayloadTypeRouterTests {

	@Test
	public void resolveExactMatch() {
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
	public void resolveSubclass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(Number.class, numberChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = numberChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void exactMatchFavoredOverSuperClass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		QueueChannel integerChannel = new QueueChannel();
		integerChannel.setBeanName("integerChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(Number.class, numberChannel);
		payloadTypeChannelMap.put(Integer.class, integerChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = integerChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(numberChannel.receive(0));
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void interfaceMatch() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel comparableChannel = new QueueChannel();
		comparableChannel.setBeanName("comparableChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(Comparable.class, comparableChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = comparableChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void directInterfaceFavoredOverSuperClass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		QueueChannel comparableChannel = new QueueChannel();
		comparableChannel.setBeanName("comparableChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(Number.class, numberChannel);
		payloadTypeChannelMap.put(Comparable.class, comparableChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = comparableChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(numberChannel.receive(0));
		assertNull(defaultChannel.receive(0));
	}

	@Test(expected = IllegalStateException.class)
	public void ambiguityFailure() throws Throwable {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel serializableChannel = new QueueChannel();
		serializableChannel.setBeanName("serializableChannel");
		QueueChannel comparableChannel = new QueueChannel();
		comparableChannel.setBeanName("comparableChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(Serializable.class, serializableChannel);
		payloadTypeChannelMap.put(Comparable.class, comparableChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message = new GenericMessage<String>("test");
		try {
			router.handleMessage(message);
		}
		catch (MessageHandlingException e) {
			throw e.getCause();
		}
	}

	@Test
	public void superClassFavoredOverIndirectInterface() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		QueueChannel serializableChannel = new QueueChannel();
		serializableChannel.setBeanName("serializableChannel");
		Map<Class<?>, MessageChannel> payloadTypeChannelMap = new ConcurrentHashMap<Class<?>, MessageChannel>();
		payloadTypeChannelMap.put(Number.class, numberChannel);
		payloadTypeChannelMap.put(Serializable.class, serializableChannel);
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setPayloadTypeChannelMap(payloadTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = numberChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(serializableChannel.receive(0));
		assertNull(defaultChannel.receive(0));
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
		router.handleMessage(message1);
		router.handleMessage(message2);
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
		router.handleMessage(message1);
		router.handleMessage(message2);
		Message<?> result1 = stringChannel.receive(25);
		assertNotNull(result1);
		assertEquals("test", result1.getPayload());
		Message<?> result2 = defaultChannel.receive(25);
		assertNotNull(result2);
		assertEquals(123, result2.getPayload());
	}

}
