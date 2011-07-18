/*
 * Copyright 2002-2010 the original author or authors.
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

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PayloadTypeRouterTests {

	@Test
	public void resolveExactMatch() {
		QueueChannel stringChannel = new QueueChannel();
		QueueChannel integerChannel = new QueueChannel();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("stringChannel", stringChannel);
		beanFactory.registerSingleton("integerChannel", integerChannel);
		
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(String.class.getName(), "stringChannel");
		payloadTypeChannelMap.put(Integer.class.getName(), "integerChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		router.setBeanFactory(beanFactory);
		
		Message<String> message1 = new GenericMessage<String>("test");
		Message<Integer> message2 = new GenericMessage<Integer>(123);
		assertEquals(1, router.getChannelIdentifiers(message1).size());
		assertEquals("stringChannel", router.getChannelIdentifiers(message1).iterator().next());
		assertEquals(1, router.getChannelIdentifiers(message2).size());
		assertEquals("integerChannel", router.getChannelIdentifiers(message2).iterator().next());
		
		// validate dynamics
		QueueChannel newChannel = new QueueChannel();
		beanFactory.registerSingleton("newChannel", newChannel);
		router.setChannelMapping(String.class.getName(), "newChannel");
		assertEquals(1, router.getChannelIdentifiers(message1).size());
		assertEquals("newChannel", router.getChannelIdentifiers(message1).iterator().next());
		// validate nothing happens if mappings were removed and resolutionRequires = false
		router.removeChannelMapping(String.class.getName());
		router.removeChannelMapping(Integer.class.getName());
		router.handleMessage(message1);
		// validate exception is thrown if mappings were removed and resolutionRequires = true
		router.setResolutionRequired(true);
		try {
			router.handleMessage(message1);
			fail();
		} catch (Exception e) {
			// ignore
		}
	}

	@Test
	public void resolveSubclass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("numberChannel", numberChannel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		router.setBeanFactory(beanFactory);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = numberChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(defaultChannel.receive(0));
		
		// validate dynamics
		QueueChannel newChannel = new QueueChannel();
		beanFactory.registerSingleton("newChannel", newChannel);
		router.setChannelMapping(Integer.class.getName(), "newChannel");
		assertEquals(1, router.getChannelIdentifiers(message).size());
		router.handleMessage(message);
		result = newChannel.receive(10);
		assertNotNull(result);
	}

	@Test
	public void exactMatchFavoredOverSuperClass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		QueueChannel integerChannel = new QueueChannel();
		integerChannel.setBeanName("integerChannel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("numberChannel", numberChannel);
		beanFactory.registerSingleton("integerChannel", integerChannel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		payloadTypeChannelMap.put(Integer.class.getName(), "integerChannel");
		
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
	
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
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("comparableChannel", comparableChannel);
			
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(Comparable.class.getName(), "comparableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
			
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = comparableChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(defaultChannel.receive(0));
	}
	
	@Test
	public void extendedInterfaceMatch() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel i2Channel = new QueueChannel();
		i2Channel.setBeanName("i2Channel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("bChannel", i2Channel);
			
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(I2.class.getName(), "bChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
			
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<C1>(new C1());
		router.handleMessage(message);
		Message<?> result = i2Channel.receive(0);
		assertNotNull(result);
	}
	
	@Test 
	public void higherWeightInterface() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		
		QueueChannel serializableChannel = new QueueChannel();
		serializableChannel.setBeanName("serializableChannel");
		
		QueueChannel i3Channel = new QueueChannel();
		i3Channel.setBeanName("i3Channel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("serializableChannel", serializableChannel);
		beanFactory.registerSingleton("i3Channel", i3Channel);
			
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		payloadTypeChannelMap.put(I3.class.getName(), "i3Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();
			
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<C1>(new C1());
		router.handleMessage(message);
	}
	
	@Test
	public void superclassWinsOverDstantInterface() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		
		QueueChannel c3Channel = new QueueChannel();
		c3Channel.setBeanName("c3Channel");
		
		QueueChannel i4Channel = new QueueChannel();
		i4Channel.setBeanName("i4Channel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("c3Channel", c3Channel);
		beanFactory.registerSingleton("i4Channel", i4Channel);
			
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(C3.class.getName(), "c3Channel");
		payloadTypeChannelMap.put(I4.class.getName(), "i4Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();
			
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<C1>(new C1());
		router.handleMessage(message);
		Message<?> result = c3Channel.receive(0);
		assertNotNull(result);
	}
	
	@Test
	public void directInterfaceOverTwoHopSuperclass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		
		QueueChannel c3Channel = new QueueChannel();
		c3Channel.setBeanName("c3Channel");
		
		QueueChannel i1AChannel = new QueueChannel();
		i1AChannel.setBeanName("i1AChannel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("c3Channel", c3Channel);
		beanFactory.registerSingleton("i1AChannel", i1AChannel);
			
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(C3.class.getName(), "c3Channel");
		payloadTypeChannelMap.put(I1A.class.getName(), "i1AChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
			
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<C1>(new C1());
		router.handleMessage(message);
		Message<?> result = i1AChannel.receive(0);
		assertNotNull(result);
	}

	@Test
	public void directInterfaceFavoredOverSuperClass() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		QueueChannel comparableChannel = new QueueChannel();
		comparableChannel.setBeanName("comparableChannel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("numberChannel", numberChannel);
		beanFactory.registerSingleton("comparableChannel", comparableChannel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		payloadTypeChannelMap.put(Comparable.class.getName(), "comparableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<Integer>(99);
		router.handleMessage(message);
		Message<?> result = comparableChannel.receive(0);
		assertNotNull(result);
		assertEquals(99, result.getPayload());
		assertNull(numberChannel.receive(0));
		assertNull(defaultChannel.receive(0));
		
		// validate dynamics
		QueueChannel newChannel = new QueueChannel();
		beanFactory.registerSingleton("newChannel", newChannel);
		router.setChannelMapping(Integer.class.getName(), "newChannel");
		assertEquals(1, router.getChannelIdentifiers(message).size());
		router.handleMessage(message);
		result = newChannel.receive(10);
		assertNotNull(result);
	}

	@Test(expected = MessageHandlingException.class)
	public void ambiguityFailure() throws Exception {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel serializableChannel = new QueueChannel();
		serializableChannel.setBeanName("serializableChannel");
		QueueChannel comparableChannel = new QueueChannel();
		comparableChannel.setBeanName("comparableChannel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("serializableChannel", serializableChannel);
		beanFactory.registerSingleton("comparableChannel", comparableChannel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		payloadTypeChannelMap.put(Comparable.class.getName(), "comparableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test
	public void superClassFavoredOverIndirectInterface() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel numberChannel = new QueueChannel();
		numberChannel.setBeanName("numberChannel");
		QueueChannel serializableChannel = new QueueChannel();
		serializableChannel.setBeanName("serializableChannel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("numberChannel", numberChannel);
		beanFactory.registerSingleton("serializableChannel", serializableChannel);
		
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
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
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("stringChannel", stringChannel);
		beanFactory.registerSingleton("integerChannel", integerChannel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(String.class.getName(), "stringChannel");
		payloadTypeChannelMap.put(Integer.class.getName(), "integerChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		Message<String> message1 = new GenericMessage<String>("test");
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
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("stringChannel", stringChannel);
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(String.class.getName(), "stringChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message1 = new GenericMessage<String>("test");
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
	
	@Test
	public void classWinningOverAmbiguateInterfacesFewLevelsDown() throws Exception {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		
		QueueChannel i4aChannel = new QueueChannel();
		i4aChannel.setBeanName("i4aChannel");
		QueueChannel i4bChannel = new QueueChannel();
		i4bChannel.setBeanName("i4bChannel");
		
		QueueChannel c2Channel = new QueueChannel();
		c2Channel.setBeanName("c2Channel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("i4aChannel", i4aChannel);
		beanFactory.registerSingleton("i4bChannel", i4aChannel);
		beanFactory.registerSingleton("c2Channel", c2Channel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(I4A.class.getName(), "i4aChannel");
		payloadTypeChannelMap.put(I4B.class.getName(), "i4bChannel");
		payloadTypeChannelMap.put(C2.class.getName(), "c2Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<C1>(new C1());
		router.handleMessage(message);
		assertNotNull(c2Channel.receive(100));
	}
	
	@Test(expected=MessageHandlingException.class) // same as above but C3 is the payload of the Message
	public void classLosesOverAmbiguateInterfacesFewLevelsDown() throws Exception {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		
		QueueChannel i4aChannel = new QueueChannel();
		i4aChannel.setBeanName("i4aChannel");
		QueueChannel i4bChannel = new QueueChannel();
		i4bChannel.setBeanName("i4bChannel");
		
		QueueChannel c2Channel = new QueueChannel();
		c2Channel.setBeanName("c2Channel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("i4aChannel", i4aChannel);
		beanFactory.registerSingleton("i4bChannel", i4aChannel);
		beanFactory.registerSingleton("c2Channel", c2Channel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(I4A.class.getName(), "i4aChannel");
		payloadTypeChannelMap.put(I4B.class.getName(), "i4bChannel");
		payloadTypeChannelMap.put(C2.class.getName(), "c2Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C3> message = new GenericMessage<C3>(new C3());
		router.handleMessage(message);
	}
	
	@Test(expected=MessageHandlingException.class)
	public void classLosingOverAmbiguateInterfacesSameLevel() throws Exception {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		
		QueueChannel i1aChannel = new QueueChannel();
		i1aChannel.setBeanName("i1aChannel");
		QueueChannel i1bChannel = new QueueChannel();
		i1bChannel.setBeanName("i1bChannel");
		
		QueueChannel c2Channel = new QueueChannel();
		c2Channel.setBeanName("c2Channel");
		
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("i1aChannel", i1aChannel);
		beanFactory.registerSingleton("i1bChannel", i1bChannel);
		beanFactory.registerSingleton("c2Channel", c2Channel);
		
		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<String, String>();
		payloadTypeChannelMap.put(I1A.class.getName(), "i1aChannel");
		payloadTypeChannelMap.put(I1B.class.getName(), "i2bChannel");
		payloadTypeChannelMap.put(C2.class.getName(), "c2Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		
		router.setBeanFactory(beanFactory);
		router.setChannelIdentifierMap(payloadTypeChannelMap);
		
		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<C1>(new C1());
		router.handleMessage(message);
	}
	
	@SuppressWarnings("serial")
	public static class C1 extends C2 implements I1A, I1B {}

	public interface I1A extends Serializable {}

	public interface I1B extends I2 {}

	public interface I2 extends I3 {}
	
	public interface I3 extends I4 {}
	
	public interface I4 extends I4A, I4B{}
	public interface I4A {}
	public interface I4B {}

	public static class C2 extends C3{}
	
	public static class C3 implements I4{}

}
