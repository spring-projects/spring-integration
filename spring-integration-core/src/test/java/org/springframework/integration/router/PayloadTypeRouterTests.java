/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.integration.router;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class PayloadTypeRouterTests {

	@Test
	public void resolveExactMatch() {
		QueueChannel stringChannel = new QueueChannel();
		QueueChannel integerChannel = new QueueChannel();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("stringChannel", stringChannel);
		beanFactory.registerSingleton("integerChannel", integerChannel);

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(String.class.getName(), "stringChannel");
		payloadTypeChannelMap.put(Integer.class.getName(), "integerChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setChannelMappings(payloadTypeChannelMap);
		router.setBeanFactory(beanFactory);

		Message<String> message1 = new GenericMessage<>("test");
		Message<Integer> message2 = new GenericMessage<>(123);
		assertThat(router.getChannelKeys(message1).size()).isEqualTo(1);

		assertThat(stringChannel.receive(0)).isNull();
		router.handleMessage(message1);
		assertThat(stringChannel.receive(0)).isEqualTo(message1);

		assertThat(router.getChannelKeys(message2).size()).isEqualTo(1);

		assertThat(integerChannel.receive(0)).isNull();
		router.handleMessage(message2);
		assertThat(integerChannel.receive(0)).isEqualTo(message2);

		// validate dynamics
		QueueChannel newChannel = new QueueChannel();
		beanFactory.registerSingleton("newChannel", newChannel);
		router.setChannelMapping(String.class.getName(), "newChannel");
		assertThat(router.getChannelKeys(message1).size()).isEqualTo(1);

		assertThat(newChannel.receive(0)).isNull();
		router.handleMessage(message1);
		assertThat(newChannel.receive(0)).isEqualTo(message1);

		// validate exception is thrown if mappings were removed and
		// channelResolutionRequires = true (which is the default)

		router.removeChannelMapping(String.class.getName());
		router.removeChannelMapping(Integer.class.getName());

		router.setResolutionRequired(true);

		try {
			router.handleMessage(message1);
			fail("Exception expected");
		}
		catch (Exception e) {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setChannelMappings(payloadTypeChannelMap);
		router.setBeanFactory(beanFactory);
		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<>(99);
		router.handleMessage(message);
		Message<?> result = numberChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(99);
		assertThat(defaultChannel.receive(0)).isNull();

		// validate dynamics
		QueueChannel newChannel = new QueueChannel();
		beanFactory.registerSingleton("newChannel", newChannel);
		router.setChannelMapping(Integer.class.getName(), "newChannel");
		assertThat(router.getChannelKeys(message).size()).isEqualTo(1);
		router.handleMessage(message);
		result = newChannel.receive(10);
		assertThat(result).isNotNull();
	}

	@Test
	public void exactMatchFavoredOverSuperclass() {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		payloadTypeChannelMap.put(Integer.class.getName(), "integerChannel");

		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<>(99);
		router.handleMessage(message);
		Message<?> result = integerChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(99);
		assertThat(numberChannel.receive(0)).isNull();
		assertThat(defaultChannel.receive(0)).isNull();
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Comparable.class.getName(), "comparableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<>(99);
		router.handleMessage(message);
		Message<?> result = comparableChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(99);
		assertThat(defaultChannel.receive(0)).isNull();
	}

	@Test
	public void extendedInterfaceMatch() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");
		QueueChannel i2Channel = new QueueChannel();
		i2Channel.setBeanName("i2Channel");

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("i2Channel", i2Channel);

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(I2.class.getName(), "i2Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		router.handleMessage(message);
		Message<?> result = i2Channel.receive(0);
		assertThat(result).isNotNull();
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		payloadTypeChannelMap.put(I3.class.getName(), "i3Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		router.handleMessage(message);
		assertThat(serializableChannel.receive(0)).isNotNull();
		assertThat(i3Channel.receive(0)).isNull();
	}

	@Test
	public void superclassWinsOverDistantInterface() {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(C3.class.getName(), "c3Channel");
		payloadTypeChannelMap.put(I4.class.getName(), "i4Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		router.handleMessage(message);
		Message<?> result = c3Channel.receive(0);
		assertThat(result).isNotNull();
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(C3.class.getName(), "c3Channel");
		payloadTypeChannelMap.put(I1A.class.getName(), "i1AChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		router.handleMessage(message);
		Message<?> result = i1AChannel.receive(0);
		assertThat(result).isNotNull();
	}

	@Test
	public void directInterfaceFavoredOverSuperclass() {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		payloadTypeChannelMap.put(Comparable.class.getName(), "comparableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<>(99);
		router.handleMessage(message);
		Message<?> result = comparableChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(99);
		assertThat(numberChannel.receive(0)).isNull();
		assertThat(defaultChannel.receive(0)).isNull();

		// validate dynamics
		QueueChannel newChannel = new QueueChannel();
		beanFactory.registerSingleton("newChannel", newChannel);
		router.setChannelMapping(Integer.class.getName(), "newChannel");
		assertThat(router.getChannelKeys(message).size()).isEqualTo(1);
		router.handleMessage(message);
		result = newChannel.receive(10);
		assertThat(result).isNotNull();
	}

	@Test
	public void ambiguityFailure() {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		payloadTypeChannelMap.put(Comparable.class.getName(), "comparableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message = new GenericMessage<>("test");
		assertThatThrownBy(() -> router.handleMessage(message))
				.isInstanceOf(MessageHandlingException.class);
	}

	@Test
	public void superclassFavoredOverIndirectInterface() {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(Number.class.getName(), "numberChannel");
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<Integer> message = new GenericMessage<>(99);
		router.handleMessage(message);
		Message<?> result = numberChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(99);
		assertThat(serializableChannel.receive(0)).isNull();
		assertThat(defaultChannel.receive(0)).isNull();
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(String.class.getName(), "stringChannel");
		payloadTypeChannelMap.put(Integer.class.getName(), "integerChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		Message<String> message1 = new GenericMessage<>("test");
		Message<Integer> message2 = new GenericMessage<>(123);
		router.handleMessage(message1);
		router.handleMessage(message2);
		Message<?> reply1 = stringChannel.receive(0);
		Message<?> reply2 = integerChannel.receive(0);
		assertThat(reply1.getPayload()).isEqualTo("test");
		assertThat(reply2.getPayload()).isEqualTo(123);
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(String.class.getName(), "stringChannel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message1 = new GenericMessage<>("test");
		Message<Integer> message2 = new GenericMessage<>(123);
		router.handleMessage(message1);
		router.handleMessage(message2);
		Message<?> result1 = stringChannel.receive(25);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test");
		Message<?> result2 = defaultChannel.receive(25);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo(123);
	}

	@Test
	public void classWinsOverMoreDistantAmbiguousInterfaces() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");

		QueueChannel i5aChannel = new QueueChannel();
		i5aChannel.setBeanName("i5aChannel");
		QueueChannel i5bChannel = new QueueChannel();
		i5bChannel.setBeanName("i5bChannel");

		QueueChannel c2Channel = new QueueChannel();
		c2Channel.setBeanName("c2Channel");

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("i5aChannel", i5aChannel);
		beanFactory.registerSingleton("i5bChannel", i5bChannel);
		beanFactory.registerSingleton("c2Channel", c2Channel);

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(I5A.class.getName(), "i5aChannel");
		payloadTypeChannelMap.put(I5B.class.getName(), "i5bChannel");
		payloadTypeChannelMap.put(C2.class.getName(), "c2Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		router.handleMessage(message);
		assertThat(c2Channel.receive(100)).isNotNull();
	}

	@Test
	public void classLosesOverLessDistantAmbiguousInterfaces() {
		QueueChannel defaultChannel = new QueueChannel();
		defaultChannel.setBeanName("defaultChannel");

		QueueChannel i2Channel = new QueueChannel();
		i2Channel.setBeanName("i2Channel");
		QueueChannel serializableChannel = new QueueChannel();
		serializableChannel.setBeanName("serializableChannel");

		QueueChannel c3Channel = new QueueChannel();
		c3Channel.setBeanName("c3Channel");

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
		beanFactory.registerSingleton("i2Channel", i2Channel);
		beanFactory.registerSingleton("serializableChannel", serializableChannel);
		beanFactory.registerSingleton("c3Channel", c3Channel);

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(I2.class.getName(), "i2Channel");
		payloadTypeChannelMap.put(Serializable.class.getName(), "serializableChannel");
		payloadTypeChannelMap.put(C3.class.getName(), "c3Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		assertThatThrownBy(() -> router.handleMessage(message))
				.isInstanceOf(MessageHandlingException.class);
	}

	@Test
	public void classLosesOverAmbiguousInterfacesAtSameLevel() {
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

		Map<String, String> payloadTypeChannelMap = new ConcurrentHashMap<>();
		payloadTypeChannelMap.put(I1A.class.getName(), "i1aChannel");
		payloadTypeChannelMap.put(I1B.class.getName(), "i2bChannel");
		payloadTypeChannelMap.put(C2.class.getName(), "c2Channel");
		PayloadTypeRouter router = new PayloadTypeRouter();

		router.setBeanFactory(beanFactory);
		router.setChannelMappings(payloadTypeChannelMap);

		router.setDefaultOutputChannel(defaultChannel);
		Message<C1> message = new GenericMessage<>(new C1());
		assertThatThrownBy(() -> router.handleMessage(message))
				.isInstanceOf(MessageHandlingException.class);
	}

	@SuppressWarnings("serial")
	public static class C1 extends C2 implements I1A, I1B {

	}

	public interface I1A extends Serializable {

	}

	public interface I1B extends I2 {

	}

	public interface I2 extends I3 {

	}

	public interface I3 extends I4 {

	}

	public interface I4 extends I5A, I5B {

	}

	public interface I5A {

	}

	public interface I5B {

	}

	public static class C2 extends C3 {

	}

	public static class C3 implements I4 {

	}

}
