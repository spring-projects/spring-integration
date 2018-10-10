/*
 * Copyright 2002-2018 the original author or authors.
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
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public class MqttAdapterTests {

	private IMqttToken alwaysComplete;

	{
		ProxyFactoryBean pfb = new ProxyFactoryBean();
		pfb.addAdvice((MethodInterceptor) invocation -> null);
		pfb.setInterfaces(IMqttToken.class);
		this.alwaysComplete = (IMqttToken) pfb.getObject();
	}

	@Test
	public void testCloseOnBadConnectIn() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		willThrow(new MqttException(0)).given(client).connect(any());
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);
		adapter.start();
		verify(client).close();
		adapter.stop();
	}

	@Test
	public void testCloseOnBadConnectOut() throws Exception {
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		willThrow(new MqttException(0)).given(client).connect(any());
		MqttPahoMessageHandler adapter = buildAdapterOut(client);
		adapter.start();
		try {
			adapter.handleMessage(new GenericMessage<>("foo"));
			fail("exception expected");
		}
		catch (MessagingException e) {
			// NOSONAR
		}
		verify(client).close();
		adapter.stop();
	}

	@Test
	public void testOutboundOptionsApplied() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(false);
		connectOptions.setConnectionTimeout(23);
		connectOptions.setKeepAliveInterval(45);
		connectOptions.setPassword("pass".toCharArray());
		MemoryPersistence persistence = new MemoryPersistence();
		factory.setPersistence(persistence);
		final SocketFactory socketFactory = mock(SocketFactory.class);
		connectOptions.setSocketFactory(socketFactory);
		final Properties props = new Properties();
		connectOptions.setSSLProperties(props);
		connectOptions.setUserName("user");
		connectOptions.setWill("foo", "bar".getBytes(), 2, true);
		factory.setConnectionOptions(connectOptions);

		factory = spy(factory);
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		willAnswer(invocation -> client).given(factory).getAsyncClientInstance(anyString(), anyString());

		MqttPahoMessageHandler handler = new MqttPahoMessageHandler("foo", "bar", factory);
		handler.setDefaultTopic("mqtt-foo");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();

		final MqttToken token = mock(MqttToken.class);
		final AtomicBoolean connectCalled = new AtomicBoolean();
		willAnswer(invocation -> {
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
		}).given(client).connect(any(MqttConnectOptions.class));
		willReturn(token).given(client).subscribe(any(String[].class), any(int[].class));

		final MqttDeliveryToken deliveryToken = mock(MqttDeliveryToken.class);
		final AtomicBoolean publishCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			assertEquals("mqtt-foo", invocation.getArguments()[0]);
			MqttMessage message = (MqttMessage) invocation.getArguments()[1];
			assertEquals("Hello, world!", new String(message.getPayload()));
			publishCalled.set(true);
			return deliveryToken;
		}).given(client).publish(anyString(), any(MqttMessage.class));

		handler.handleMessage(new GenericMessage<String>("Hello, world!"));

		verify(client, times(1)).connect(any(MqttConnectOptions.class));
		assertTrue(connectCalled.get());
	}

	@Test
	public void testInboundOptionsApplied() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(false);
		connectOptions.setConnectionTimeout(23);
		connectOptions.setKeepAliveInterval(45);
		connectOptions.setPassword("pass".toCharArray());
		MemoryPersistence persistence = new MemoryPersistence();
		factory.setPersistence(persistence);
		final SocketFactory socketFactory = mock(SocketFactory.class);
		connectOptions.setSocketFactory(socketFactory);
		final Properties props = new Properties();
		connectOptions.setSSLProperties(props);
		connectOptions.setUserName("user");
		connectOptions.setWill("foo", "bar".getBytes(), 2, true);
		factory.setConnectionOptions(connectOptions);

		factory = spy(factory);
		final IMqttClient client = mock(IMqttClient.class);
		willAnswer(invocation -> client).given(factory).getClientInstance(anyString(), anyString());

		final AtomicBoolean connectCalled = new AtomicBoolean();
		final AtomicBoolean failConnection = new AtomicBoolean();
		final CountDownLatch waitToFail = new CountDownLatch(1);
		final CountDownLatch failInProcess = new CountDownLatch(1);
		final CountDownLatch goodConnection = new CountDownLatch(2);
		final MqttException reconnectException = new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);
		willAnswer(invocation -> {
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
			return null;
		}).given(client).connect(any(MqttConnectOptions.class));

		final AtomicReference<MqttCallback> callback = new AtomicReference<MqttCallback>();
		willAnswer(invocation -> {
			callback.set((MqttCallback) invocation.getArguments()[0]);
			return null;
		}).given(client).setCallback(any(MqttCallback.class));

		given(client.isConnected()).willReturn(true);

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
		willAnswer(invocation -> {
			events.add((MqttIntegrationEvent) invocation.getArguments()[0]);
			return null;
		}).given(applicationEventPublisher).publishEvent(any(MqttIntegrationEvent.class));
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
		taskScheduler.destroy();
	}

	@Test
	public void testStopActionDefault() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, null, null);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);
	}

	@Test
	public void testStopActionDefaultNotClean() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, false, null);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@Test
	public void testStopActionAlways() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, false,
				ConsumerStopAction.UNSUBSCRIBE_ALWAYS);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);

		adapter.connectionLost(new RuntimeException("Intentional"));

		TaskScheduler taskScheduler = TestUtils.getPropertyValue(adapter, "taskScheduler", TaskScheduler.class);

		verify(taskScheduler, never())
				.schedule(any(Runnable.class), any(Date.class));
	}

	@Test
	public void testStopActionNever() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@Test
	public void testReconnect() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);
		adapter.setRecoveryInterval(10);
		Log logger = spy(TestUtils.getPropertyValue(adapter, "logger", Log.class));
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		given(logger.isDebugEnabled()).willReturn(true);
		final AtomicInteger attemptingReconnectCount = new AtomicInteger();
		willAnswer(i -> {
			if (attemptingReconnectCount.getAndIncrement() == 0) {
				adapter.connectionLost(new RuntimeException("while schedule running"));
			}
			i.callRealMethod();
			return null;
		}).given(logger).debug("Attempting reconnect");
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		adapter.connectionLost(new RuntimeException("initial"));
		verify(client).close();
		Thread.sleep(1000);
		// the following assertion should be equalTo, but leq to protect against a slow CI server
		assertThat(attemptingReconnectCount.get(), lessThanOrEqualTo(2));
		adapter.stop();
		taskScheduler.destroy();
	}

	private MqttPahoMessageDrivenChannelAdapter buildAdapterIn(final IMqttClient client, Boolean cleanSession,
			ConsumerStopAction action) throws MqttException {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory() {

			@Override
			public IMqttClient getClientInstance(String uri, String clientId) throws MqttException {
				return client;
			}

		};
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[] { "tcp://localhost:1883" });
		if (cleanSession != null) {
			connectOptions.setCleanSession(cleanSession);
		}
		if (action != null) {
			factory.setConsumerStopAction(action);
		}
		factory.setConnectionOptions(connectOptions);
		given(client.isConnected()).willReturn(true);
		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("client", factory, "foo");
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.setOutputChannel(new NullChannel());
		adapter.setTaskScheduler(mock(TaskScheduler.class));
		adapter.afterPropertiesSet();
		return adapter;
	}

	private MqttPahoMessageHandler buildAdapterOut(final IMqttAsyncClient client) throws MqttException {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory() {

			@Override
			public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) throws MqttException {
				return client;
			}

		};
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[] { "tcp://localhost:1883" });
		factory.setConnectionOptions(connectOptions);
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("client", factory);
		adapter.setDefaultTopic("foo");
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.afterPropertiesSet();
		return adapter;
	}

	private void verifyUnsubscribe(IMqttClient client) throws Exception {
		verify(client).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(any(String[].class), any(int[].class));
		verify(client).unsubscribe(any(String[].class));
		verify(client).disconnectForcibly(anyLong());
	}

	private void verifyNotUnsubscribe(IMqttClient client) throws Exception {
		verify(client).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(any(String[].class), any(int[].class));
		verify(client, never()).unsubscribe(any(String[].class));
		verify(client).disconnectForcibly(anyLong());
	}

}
