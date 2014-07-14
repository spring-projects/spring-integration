/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory.Will;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public class MqttAdapterTests {

	@Test
	public void testPahoConnectOptions() {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setCleanSession(false);
		factory.setConnectionTimeout(23);
		factory.setKeepAliveInterval(45);
		factory.setPassword("pass");
		SocketFactory socketFactory = mock(SocketFactory.class);
		factory.setSocketFactory(socketFactory);
		Properties props = new Properties();
		factory.setSslProperties(props);
		factory.setUserName("user");
		Will will = new Will("foo", "bar".getBytes(), 2, true);
		factory.setWill(will);

		MqttConnectOptions options = factory.getConnectionOptions();

		assertEquals(23, options.getConnectionTimeout());
		assertEquals(45, options.getKeepAliveInterval());
		assertEquals("pass", new String(options.getPassword()));
		assertSame(socketFactory, options.getSocketFactory());
		assertSame(props, options.getSSLProperties());
		assertEquals("user", options.getUserName());
		assertEquals("foo", options.getWillDestination());
		assertEquals("bar", new String(options.getWillMessage().getPayload()));
		assertEquals(2, options.getWillMessage().getQos());

	}

	@Test
	public void testOutboundOptionsApplied() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setCleanSession(false);
		factory.setConnectionTimeout(23);
		factory.setKeepAliveInterval(45);
		factory.setPassword("pass");
		MemoryPersistence persistence = new MemoryPersistence();
		factory.setPersistence(persistence);
		final SocketFactory socketFactory = mock(SocketFactory.class);
		factory.setSocketFactory(socketFactory);
		final Properties props = new Properties();
		factory.setSslProperties(props);
		factory.setUserName("user");
		Will will = new Will("foo", "bar".getBytes(), 2, true);
		factory.setWill(will);

		factory = spy(factory);
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		doAnswer(new Answer<MqttAsyncClient>() {

			@Override
			public MqttAsyncClient answer(InvocationOnMock invocation) throws Throwable {
				return client;
			}
		}).when(factory).getAsyncClientInstance(anyString(), anyString());

		MqttPahoMessageHandler handler = new MqttPahoMessageHandler("foo", "bar", factory);
		handler.setDefaultTopic("mqtt-foo");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();

		final MqttToken token = mock(MqttToken.class);
		final AtomicBoolean connectCalled = new AtomicBoolean();
		doAnswer(new Answer<MqttToken>(){

			@Override
			public MqttToken answer(InvocationOnMock invocation) throws Throwable {
				MqttConnectOptions options = (MqttConnectOptions) invocation.getArguments()[0];
				assertEquals(23, options.getConnectionTimeout());
				assertEquals(45, options.getKeepAliveInterval());
				assertEquals("pass", new String(options.getPassword()));
				assertSame(socketFactory, options.getSocketFactory());
				assertSame(props, options.getSSLProperties());
				assertEquals("user", options.getUserName());
				assertEquals("foo", options.getWillDestination());
				assertEquals("bar", new String(options.getWillMessage().getPayload()));
				assertEquals(2, options.getWillMessage().getQos());
				connectCalled.set(true);
				return token;
			}
		}).when(client).connect(any(MqttConnectOptions.class));
		doReturn(token).when(client).subscribe(any(String[].class), any(int[].class));

		final MqttDeliveryToken deliveryToken = mock(MqttDeliveryToken.class);
		final AtomicBoolean publishCalled = new AtomicBoolean();
		doAnswer(new Answer<MqttDeliveryToken>() {

			@Override
			public MqttDeliveryToken answer(InvocationOnMock invocation) throws Throwable {
				assertEquals("mqtt-foo", invocation.getArguments()[0]);
				MqttMessage message = (MqttMessage) invocation.getArguments()[1];
				assertEquals("Hello, world!", new String(message.getPayload()));
				publishCalled.set(true);
				return deliveryToken;
			}
		}).when(client).publish(anyString(), any(MqttMessage.class));

		handler.handleMessage(new GenericMessage<String>("Hello, world!"));

		verify(client, times(1)).connect(any(MqttConnectOptions.class));
		assertTrue(connectCalled.get());
	}

	@Test
	public void testInboundOptionsApplied() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setCleanSession(false);
		factory.setConnectionTimeout(23);
		factory.setKeepAliveInterval(45);
		factory.setPassword("pass");
		MemoryPersistence persistence = new MemoryPersistence();
		factory.setPersistence(persistence);
		final SocketFactory socketFactory = mock(SocketFactory.class);
		factory.setSocketFactory(socketFactory);
		final Properties props = new Properties();
		factory.setSslProperties(props);
		factory.setUserName("user");
		Will will = new Will("foo", "bar".getBytes(), 2, true);
		factory.setWill(will);

		factory = spy(factory);
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		doAnswer(new Answer<MqttAsyncClient>() {

			@Override
			public MqttAsyncClient answer(InvocationOnMock invocation) throws Throwable {
				return client;
			}
		}).when(factory).getAsyncClientInstance(anyString(), anyString());

		final MqttToken token = mock(MqttToken.class);
		final AtomicBoolean connectCalled = new AtomicBoolean();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				MqttConnectOptions options = (MqttConnectOptions) invocation.getArguments()[0];
				assertEquals(23, options.getConnectionTimeout());
				assertEquals(45, options.getKeepAliveInterval());
				assertEquals("pass", new String(options.getPassword()));
				assertSame(socketFactory, options.getSocketFactory());
				assertSame(props, options.getSSLProperties());
				assertEquals("user", options.getUserName());
				assertEquals("foo", options.getWillDestination());
				assertEquals("bar", new String(options.getWillMessage().getPayload()));
				assertEquals(2, options.getWillMessage().getQos());
				connectCalled.set(true);
				return token;
			}
		}).when(client).connect(any(MqttConnectOptions.class));
		doReturn(token).when(client).subscribe(any(String[].class), any(int[].class));

		final AtomicReference<MqttCallback> callback = new AtomicReference<MqttCallback>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				callback.set((MqttCallback) invocation.getArguments()[0]);
				return null;
			}
		}).when(client).setCallback(any(MqttCallback.class));

		when(client.isConnected()).thenReturn(true);

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("foo", "bar", factory, "baz");
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();

		verify(client, times(1)).connect(any(MqttConnectOptions.class));
		assertTrue(connectCalled.get());

		MqttMessage message = new MqttMessage("qux".getBytes());
		callback.get().messageArrived("baz", message);
		Message<?> outMessage = outputChannel.receive(0);
		assertNotNull(outMessage);
		assertEquals("qux", outMessage.getPayload());
	}

}
