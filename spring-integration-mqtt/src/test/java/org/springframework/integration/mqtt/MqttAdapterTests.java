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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.CallsRealMethods;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils;

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
	public void testCloseOnBadConnect() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		willThrow(new MqttException(0)).given(client).connect(any());
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);
		adapter.start();
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
		final SocketFactory socketFactory = SocketFactory.getDefault();
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
			MqttConnectOptions options = invocation.getArgument(0);
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
			MqttMessage message = invocation.getArgument(1);
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
		final SocketFactory socketFactory = SocketFactory.getDefault();
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
			MqttConnectOptions options = invocation.getArgument(0);
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
			callback.set(invocation.getArgument(0));
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
			events.add(invocation.getArgument(0));
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
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, null, null);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);
	}

	@Test
	public void testStopActionDefaultNotClean() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, false, null);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@Test
	public void testStopActionAlways() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, false,
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
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCustomExpressions() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		MqttPahoMessageHandler handler = ctx.getBean("handler", MqttPahoMessageHandler.class);
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertEquals("fooTopic",
				TestUtils.getPropertyValue(handler, "topicProcessor", MessageProcessor.class).processMessage(message));
		assertEquals(1,
				TestUtils.getPropertyValue(handler, "converter.qosProcessor", MessageProcessor.class)
						.processMessage(message));
		assertEquals(Boolean.TRUE,
				TestUtils.getPropertyValue(handler, "converter.retainedProcessor", MessageProcessor.class)
						.processMessage(message));

		handler = ctx.getBean("handlerWithNullExpressions", MqttPahoMessageHandler.class);
		assertEquals(1,
				TestUtils.getPropertyValue(handler, "converter", DefaultPahoMessageConverter.class)
						.fromMessage(message, null).getQos());
		assertEquals(Boolean.TRUE,
				TestUtils.getPropertyValue(handler, "converter", DefaultPahoMessageConverter.class)
						.fromMessage(message, null).isRetained());
		ctx.close();
	}

	@Test
	public void testReconnect() throws Exception {
		final IMqttClient client = mock(IMqttClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapter(client, null, ConsumerStopAction.UNSUBSCRIBE_NEVER);
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

	@Test
	public void testSubscribeFailure() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(false);
		connectOptions.setConnectionTimeout(23);
		connectOptions.setKeepAliveInterval(45);
		connectOptions.setPassword("pass".toCharArray());
		MemoryPersistence persistence = new MemoryPersistence();
		factory.setPersistence(persistence);
		final SocketFactory socketFactory = SocketFactory.getDefault();
		connectOptions.setSocketFactory(socketFactory);
		final Properties props = new Properties();
		connectOptions.setSSLProperties(props);
		connectOptions.setUserName("user");
		connectOptions.setWill("foo", "bar".getBytes(), 2, true);

		factory = spy(factory);
		MqttAsyncClient aClient = mock(MqttAsyncClient.class);
		final MqttClient client = mock(MqttClient.class);
		willAnswer(invocation -> client).given(factory).getClientInstance(anyString(), anyString());
		given(client.isConnected()).willReturn(true);
		new DirectFieldAccessor(client).setPropertyValue("aClient", aClient);
		willAnswer(new CallsRealMethods()).given(client).connect(any(MqttConnectOptions.class));
		willAnswer(new CallsRealMethods()).given(client).subscribe(any(String[].class), any(int[].class));
		willReturn(alwaysComplete).given(aClient).connect(any(MqttConnectOptions.class), any(), any());

		IMqttToken token = mock(IMqttToken.class);
		given(token.getGrantedQos()).willReturn(new int[] { 0x80 });
		willReturn(token).given(aClient).subscribe(any(String[].class), any(int[].class), any(), any());

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("foo", "bar", factory,
				"baz", "fix");
		AtomicReference<Method> method = new AtomicReference<>();
		ReflectionUtils.doWithMethods(MqttPahoMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);
		}, m -> m.getName().equals("connectAndSubscribe"));
		assertNotNull(method.get());
		try {
			method.get().invoke(adapter);
			fail("Expected InvocationTargetException");
		}
		catch (InvocationTargetException e) {
			assertThat(e.getCause(), instanceOf(MqttException.class));
			assertThat(((MqttException) e.getCause()).getReasonCode(),
					equalTo((int) MqttException.REASON_CODE_SUBSCRIBE_FAILED));
		}
	}

	@Test
	public void testDifferentQos() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(false);
		connectOptions.setConnectionTimeout(23);
		connectOptions.setKeepAliveInterval(45);
		connectOptions.setPassword("pass".toCharArray());
		MemoryPersistence persistence = new MemoryPersistence();
		factory.setPersistence(persistence);
		final SocketFactory socketFactory = SocketFactory.getDefault();
		connectOptions.setSocketFactory(socketFactory);
		final Properties props = new Properties();
		connectOptions.setSSLProperties(props);
		connectOptions.setUserName("user");
		connectOptions.setWill("foo", "bar".getBytes(), 2, true);

		factory = spy(factory);
		MqttAsyncClient aClient = mock(MqttAsyncClient.class);
		final MqttClient client = mock(MqttClient.class);
		willAnswer(invocation -> client).given(factory).getClientInstance(anyString(), anyString());
		given(client.isConnected()).willReturn(true);
		new DirectFieldAccessor(client).setPropertyValue("aClient", aClient);
		willAnswer(new CallsRealMethods()).given(client).connect(any(MqttConnectOptions.class));
		willAnswer(new CallsRealMethods()).given(client).subscribe(any(String[].class), any(int[].class));
		willReturn(alwaysComplete).given(aClient).connect(any(MqttConnectOptions.class), any(), any());

		IMqttToken token = mock(IMqttToken.class);
		given(token.getGrantedQos()).willReturn(new int[] { 2, 0 });
		willReturn(token).given(aClient).subscribe(any(String[].class), any(int[].class), any(), any());

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("foo", "bar", factory,
				"baz", "fix");
		AtomicReference<Method> method = new AtomicReference<>();
		ReflectionUtils.doWithMethods(MqttPahoMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);
		}, m -> m.getName().equals("connectAndSubscribe"));
		assertNotNull(method.get());
		Log logger = spy(TestUtils.getPropertyValue(adapter, "logger", Log.class));
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		given(logger.isWarnEnabled()).willReturn(true);
		method.get().invoke(adapter);
		verify(logger, atLeastOnce())
				.warn("Granted QOS different to Requested QOS; topics: [baz, fix] requested: [1, 1] granted: [2, 0]");
		verify(client).setTimeToWait(30_000L);
	}

	private MqttPahoMessageDrivenChannelAdapter buildAdapter(final IMqttClient client, Boolean cleanSession,
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

	@Configuration
	public static class Config {

		@Bean
		public MqttPahoMessageHandler handler() {
			MqttPahoMessageHandler handler = new MqttPahoMessageHandler("tcp://localhost:1883", "bar");
			handler.setTopicExpressionString("@topic");
			handler.setQosExpressionString("@qos");
			handler.setRetainedExpressionString("@retained");
			return handler;
		}

		@Bean
		public String topic() {
			return "fooTopic";
		}

		@Bean
		public Integer qos() {
			return 1;
		}

		@Bean
		public Boolean retained() {
			return true;
		}

		@Bean
		public MqttPahoMessageHandler handlerWithNullExpressions() {
			MqttPahoMessageHandler handler = new MqttPahoMessageHandler("tcp://localhost:1883", "bar");
			handler.setDefaultQos(1);
			handler.setQosExpressionString("null");
			handler.setDefaultRetained(true);
			handler.setRetainedExpressionString("null");
			return handler;
		}

	}

}
