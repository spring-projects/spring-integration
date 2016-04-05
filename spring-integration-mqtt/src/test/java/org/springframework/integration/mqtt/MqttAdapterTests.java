/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory.Will;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
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

	private IMqttToken alwaysComplete;

	{
		ProxyFactoryBean pfb = new ProxyFactoryBean();
		pfb.addAdvice(new MethodInterceptor() {

			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				return null;
			}

		});
		pfb.setInterfaces(IMqttToken.class);
		this.alwaysComplete = (IMqttToken) pfb.getObject();
	}

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
		doAnswer(new Answer<MqttToken>() {

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
		final AtomicBoolean failConnection = new AtomicBoolean();
		final CountDownLatch waitToFail = new CountDownLatch(1);
		final CountDownLatch failInProcess = new CountDownLatch(1);
		final CountDownLatch goodConnection = new CountDownLatch(2);
		final MqttException reconnectException = new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (failConnection.get()) {
					failInProcess.countDown();
					waitToFail.await(10, TimeUnit.SECONDS);
					throw reconnectException;
				}
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
				goodConnection.countDown();
				return token;
			}
		}).when(client).connect(any(MqttConnectOptions.class));
		doReturn(token).when(client).subscribe(any(String[].class), any(int[].class));
		doReturn(token).when(client).disconnect();

		final AtomicReference<MqttCallback> callback = new AtomicReference<MqttCallback>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				callback.set((MqttCallback) invocation.getArguments()[0]);
				return null;
			}
		}).when(client).setCallback(any(MqttCallback.class));

		when(client.isConnected()).thenReturn(true);

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("foo", "bar", factory,
				"baz", "fix");
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.setBeanFactory(mock(BeanFactory.class));
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		final BlockingQueue<MqttIntegrationEvent> events = new LinkedBlockingQueue<MqttIntegrationEvent>();
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				events.add((MqttIntegrationEvent) invocation.getArguments()[0]);
				return null;
			}
		}).when(applicationEventPublisher).publishEvent(any(MqttIntegrationEvent.class));
		adapter.setApplicationEventPublisher(applicationEventPublisher);
		adapter.setRecoveryInterval(500);
		adapter.afterPropertiesSet();
		adapter.start();

		verify(client, times(1)).connect(any(MqttConnectOptions.class));
		assertTrue(connectCalled.get());

		MqttMessage message = new MqttMessage("qux".getBytes());
		callback.get().messageArrived("baz", message);
		Message<?> outMessage = outputChannel.receive(0);
		assertNotNull(outMessage);
		assertEquals("qux", outMessage.getPayload());

		MqttIntegrationEvent event = events.poll(10, TimeUnit.SECONDS);
		assertThat(event, instanceOf(MqttSubscribedEvent.class));
		assertEquals("Connected and subscribed to [baz, fix]", ((MqttSubscribedEvent) event).getMessage());

		// lose connection and make first reconnect fail
		failConnection.set(true);
		RuntimeException e = new RuntimeException("foo");
		adapter.connectionLost(e);

		event = events.poll(10, TimeUnit.SECONDS);
		assertThat(event, instanceOf(MqttConnectionFailedEvent.class));
		assertSame(event.getCause(), e);

		assertTrue(failInProcess.await(10, TimeUnit.SECONDS));
		waitToFail.countDown();
		failConnection.set(false);
		event = events.poll(10, TimeUnit.SECONDS);
		assertThat(event, instanceOf(MqttConnectionFailedEvent.class));
		assertSame(event.getCause(), reconnectException);

		// reconnect can now succeed; however, we might have other failures on a slow server (500ms retry).
		assertTrue(goodConnection.await(10, TimeUnit.SECONDS));
		int n = 0;
		while (!(event instanceof MqttSubscribedEvent) && n++ < 20) {
			event = events.poll(10, TimeUnit.SECONDS);
		}
		assertThat(event, instanceOf(MqttSubscribedEvent.class));
		assertEquals("Connected and subscribed to [baz, fix]", ((MqttSubscribedEvent) event).getMessage());
	}

	@Test
	public void testStopActionDefault() throws Exception {
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, null, null);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);
	}

	@Test
	public void testStopActionDefaultNotClean() throws Exception {
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, false, null);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@Test
	public void testStopActionAlways() throws Exception {
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, false,
				ConsumerStopAction.UNSUBSCRIBE_ALWAYS);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);
	}

	@Test
	public void testStopActionNever() throws Exception {
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	private MqttPahoMessageDrivenChannelAdapter buildAdapter(final MqttAsyncClient client, Boolean cleanSession,
			ConsumerStopAction action) throws MqttException, MqttSecurityException {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory() {

			@Override
			public MqttAsyncClient getAsyncClientInstance(String uri, String clientId) throws MqttException {
				return client;
			}

		};
		factory.setServerURIs("tcp://localhost:1883");
		if (cleanSession != null) {
			factory.setCleanSession(cleanSession);
		}
		if (action != null) {
			factory.setConsumerStopAction(action);
		}
		when(client.connect(any(MqttConnectOptions.class))).thenReturn(this.alwaysComplete);
		when(client.subscribe(any(String[].class), any(int[].class))).thenReturn(this.alwaysComplete);
		when(client.disconnect()).thenReturn(this.alwaysComplete);
		when(client.unsubscribe(any(String[].class))).thenReturn(this.alwaysComplete);
		when(client.isConnected()).thenReturn(true);
		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("client", factory, "foo");
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.setOutputChannel(new NullChannel());
		adapter.afterPropertiesSet();
		return adapter;
	}

	private void verifyUnsubscribe(MqttAsyncClient client) throws Exception {
		verify(client).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(any(String[].class), any(int[].class));
		verify(client).unsubscribe(any(String[].class));
		verify(client).disconnect();
	}

	private void verifyNotUnsubscribe(MqttAsyncClient client) throws Exception {
		verify(client).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(any(String[].class), any(int[].class));
		verify(client, never()).unsubscribe(any(String[].class));
		verify(client).disconnect();
	}

}
