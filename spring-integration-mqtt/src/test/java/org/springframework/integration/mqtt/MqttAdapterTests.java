/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.mqtt;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.net.SocketFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.core.Mqttv3ClientManager;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaderAccessor;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 *
 * @since 4.0
 *
 */
public class MqttAdapterTests implements TestApplicationContextAware {

	private final IMqttToken alwaysComplete;

	{
		ProxyFactoryBean pfb = new ProxyFactoryBean();
		pfb.addAdvice((MethodInterceptor) invocation -> null);
		pfb.setInterfaces(IMqttToken.class);
		this.alwaysComplete = (IMqttToken) pfb.getObject();
	}

	@Test
	public void testCloseOnBadConnectOut() throws Exception {
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		MqttPahoMessageHandler adapter = buildAdapterOut(client);
		willThrow(new MqttException(0)).given(client).connect(any());
		adapter.start();
		try {
			adapter.handleMessage(new GenericMessage<>("foo"));
			fail("exception expected");
		}
		catch (MessageHandlingException e) {
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
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		handler.start();

		final MqttToken token = mock(MqttToken.class);
		final AtomicBoolean connectCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			MqttConnectOptions options = invocation.getArgument(0);
			assertThat(options.getConnectionTimeout()).isEqualTo(23);
			assertThat(options.getKeepAliveInterval()).isEqualTo(45);
			assertThat(new String(options.getPassword())).isEqualTo("pass");
			assertThat(options.getSocketFactory()).isSameAs(socketFactory);
			assertThat(options.getSSLProperties()).isSameAs(props);
			assertThat(options.getUserName()).isEqualTo("user");
			assertThat(options.getWillDestination()).isEqualTo("foo");
			assertThat(new String(options.getWillMessage().getPayload())).isEqualTo("bar");
			assertThat(options.getWillMessage().getQos()).isEqualTo(2);
			connectCalled.set(true);
			return token;
		}).given(client).connect(any(MqttConnectOptions.class));
		willReturn(token).given(client).subscribe(any(String[].class), any(int[].class), any());

		final MqttDeliveryToken deliveryToken = mock(MqttDeliveryToken.class);
		final AtomicBoolean publishCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			assertThat(invocation.getArguments()[0]).isEqualTo("mqtt-foo");
			MqttMessage message = invocation.getArgument(1);
			assertThat(new String(message.getPayload())).isEqualTo("Hello, world!");
			publishCalled.set(true);
			return deliveryToken;
		}).given(client).publish(anyString(), any(), any(), any());

		handler.handleMessage(new GenericMessage<>("Hello, world!"));

		verify(client, times(1)).connect(any(MqttConnectOptions.class));
		assertThat(connectCalled.get()).isTrue();
		AtomicReference<Object> failed = new AtomicReference<>();
		handler.setApplicationEventPublisher(failed::set);
		handler.connectionLost(new IllegalStateException());
		assertThat(failed.get()).isInstanceOf(MqttConnectionFailedEvent.class);
		handler.stop();
	}

	@Test
	void testClientManagerIsNotConnectedAndClosedInHandler() throws Exception {
		// given
		var clientManager = mock(Mqttv3ClientManager.class);
		when(clientManager.getConnectionInfo()).thenReturn(new MqttConnectOptions());
		var client = mock(MqttAsyncClient.class);
		given(clientManager.getClient()).willReturn(client);

		var deliveryToken = mock(MqttDeliveryToken.class);
		given(client.publish(anyString(), any(), any(), any())).willReturn(deliveryToken);

		var handler = new MqttPahoMessageHandler(clientManager);
		handler.setDefaultTopic("mqtt-foo");
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		handler.start();

		// when
		handler.handleMessage(new GenericMessage<>("Hello, world!"));
		handler.stop();

		// then
		verify(client, never()).connect(any(MqttConnectOptions.class));
		verify(client).publish(anyString(), any(), any(), any());
		verify(client, never()).disconnect();
		verify(client, never()).disconnect(anyLong());
		verify(client, never()).close();
	}

	@Test
	void testClientManagerIsNotConnectedAndClosedInAdapter() throws Exception {
		// given
		var clientManager = mock(Mqttv3ClientManager.class);
		when(clientManager.getConnectionInfo()).thenReturn(new MqttConnectOptions());
		var client = mock(MqttAsyncClient.class);
		given(clientManager.getClient()).willReturn(client);

		var subscribeToken = mock(MqttToken.class);
		given(subscribeToken.getGrantedQos()).willReturn(new int[] {2});
		given(client.subscribe(any(String[].class), any(int[].class), any()))
				.willReturn(subscribeToken);

		var adapter = new MqttPahoMessageDrivenChannelAdapter(clientManager, "mqtt-foo");
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();

		// when
		adapter.start();
		adapter.connectComplete(false, null);
		adapter.stop();

		// then
		verify(client, never()).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(eq(new String[] {"mqtt-foo"}), any(int[].class), any());
		verify(client).unsubscribe(new String[] {"mqtt-foo"});
		verify(client, never()).disconnect();
		verify(client, never()).disconnect(anyLong());
		verify(client, never()).close();
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
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		willReturn(client).given(factory).getAsyncClientInstance(anyString(), anyString());

		final AtomicBoolean connectCalled = new AtomicBoolean();
		IMqttToken token = mock(IMqttToken.class);
		willAnswer(invocation -> {
			MqttConnectOptions options = invocation.getArgument(0);
			assertThat(options.getConnectionTimeout()).isEqualTo(23);
			assertThat(options.getKeepAliveInterval()).isEqualTo(45);
			assertThat(new String(options.getPassword())).isEqualTo("pass");
			assertThat(options.getSocketFactory()).isSameAs(socketFactory);
			assertThat(options.getSSLProperties()).isSameAs(props);
			assertThat(options.getUserName()).isEqualTo("user");
			assertThat(options.getWillDestination()).isEqualTo("foo");
			assertThat(new String(options.getWillMessage().getPayload())).isEqualTo("bar");
			assertThat(options.getWillMessage().getQos()).isEqualTo(2);
			connectCalled.set(true);
			return token;
		}).given(client).connect(any(MqttConnectOptions.class));
		given(client.subscribe(any(String[].class), any(int[].class), any())).willReturn(token);
		given(token.getGrantedQos()).willReturn(new int[] {2});

		final AtomicReference<MqttCallbackExtended> callback = new AtomicReference<>();
		willAnswer(invocation -> {
			callback.set(invocation.getArgument(0));
			return null;
		}).given(client).setCallback(any(MqttCallbackExtended.class));

		given(client.isConnected()).willReturn(true);

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("foo", "bar", factory,
				"baz", "fix");
		adapter.setManualAcks(true);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		final BlockingQueue<MqttIntegrationEvent> events = new LinkedBlockingQueue<>();
		willAnswer(invocation -> {
			events.add(invocation.getArgument(0));
			return null;
		}).given(applicationEventPublisher).publishEvent(any(MqttIntegrationEvent.class));
		adapter.setApplicationEventPublisher(applicationEventPublisher);
		adapter.afterPropertiesSet();
		adapter.start();
		adapter.connectComplete(false, null);

		verify(client, times(1)).connect(any(MqttConnectOptions.class));
		assertThat(connectCalled.get()).isTrue();

		MqttMessage message = new MqttMessage("qux".getBytes());
		callback.get().messageArrived("baz", message);
		Message<?> outMessage = outputChannel.receive(0);
		assertThat(outMessage).isNotNull();
		assertThat(outMessage.getPayload()).isEqualTo("qux");

		StaticMessageHeaderAccessor.getAcknowledgment(outMessage).acknowledge();
		verify(client).setManualAcks(true);
		verify(client)
				.messageArrivedComplete(MqttHeaderAccessor.id(outMessage), MqttHeaderAccessor.receivedQos(outMessage));

		MqttIntegrationEvent event = events.poll(10, TimeUnit.SECONDS);
		assertThat(event).isInstanceOf(MqttSubscribedEvent.class);
		assertThat(((MqttSubscribedEvent) event).getMessage()).isEqualTo("Connected and subscribed to [baz, fix]");

		adapter.setConverter(new MqttMessageConverter() {

			@Override
			public AbstractIntegrationMessageBuilder<?> toMessageBuilder(String topic, MqttMessage mqttMessage) {
				return null;
			}

			@Override
			public Object fromMessage(Message<?> message, Class<?> targetClass) {
				return null;
			}

			@Override
			public Message<?> toMessage(Object payload, MessageHeaders headers) {
				return null;
			}

		});

		callback.get().messageArrived("baz", message);

		ErrorMessage errorMessage = (ErrorMessage) errorChannel.receive(0);
		assertThat(errorMessage).isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(IllegalStateException.class);
		IllegalStateException exception = (IllegalStateException) errorMessage.getPayload();
		assertThat(exception).hasMessage("'MqttMessageConverter' returned 'null'");
		assertThat(errorMessage.getOriginalMessage().getPayload()).isSameAs(message);
	}

	@Test
	public void testStopActionDefault() throws Exception {
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, null);

		adapter.start();
		adapter.connectComplete(false, null);
		adapter.stop();
		verifyUnsubscribe(client);
	}

	@Test
	public void testStopActionDefaultNotClean() throws Exception {
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		MqttPahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, false);

		adapter.start();
		adapter.connectComplete(false, null);
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCustomExpressions() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		MqttPahoMessageHandler handler = ctx.getBean("handler", MqttPahoMessageHandler.class);
		GenericMessage<String> message = new GenericMessage<>("foo");
		handler.setApplicationContext(ctx);
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(handler, "topicProcessor")
				.processMessage(message)).isEqualTo("fooTopic");
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(handler, "converter.qosProcessor")
				.processMessage(message)).isEqualTo(1);
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(handler, "converter.retainedProcessor")
				.processMessage(message)).isEqualTo(Boolean.TRUE);

		handler = ctx.getBean("handlerWithNullExpressions", MqttPahoMessageHandler.class);
		assertThat(TestUtils.<DefaultPahoMessageConverter>getPropertyValue(handler, "converter")
				.fromMessage(message, null).getQos()).isEqualTo(1);
		assertThat(TestUtils.<DefaultPahoMessageConverter>getPropertyValue(handler, "converter")
				.fromMessage(message, null).isRetained()).isEqualTo(Boolean.TRUE);
		ctx.close();
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
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		willReturn(client).given(factory).getAsyncClientInstance(anyString(), anyString());
		given(client.isConnected()).willReturn(true);
		willReturn(alwaysComplete).given(client).connect(any(MqttConnectOptions.class));

		IMqttToken token = mock(IMqttToken.class);
		given(token.getGrantedQos()).willReturn(new int[] {0x80});
		willReturn(token).given(client).subscribe(any(String[].class), any(int[].class), any());

		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("foo", "bar", factory,
				"baz", "fix");
		AtomicReference<Method> method = new AtomicReference<>();
		ReflectionUtils.doWithMethods(MqttPahoMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);
		}, m -> m.getName().equals("connect"));
		assertThat(method.get()).isNotNull();
		method.get().invoke(adapter);
		ReflectionUtils.doWithMethods(MqttPahoMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);
		}, m -> m.getName().equals("subscribe"));
		assertThat(method.get()).isNotNull();
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		adapter.setApplicationEventPublisher(eventPublisher);
		method.get().invoke(adapter);
		verify(eventPublisher).publishEvent(any(MqttConnectionFailedEvent.class));
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
		final MqttAsyncClient client = mock(MqttAsyncClient.class);
		willReturn(client).given(factory).getAsyncClientInstance(anyString(), anyString());
		given(client.isConnected()).willReturn(true);
		willReturn(alwaysComplete).given(client).connect(any(MqttConnectOptions.class));

		IMqttToken token = mock(IMqttToken.class);
		given(token.getGrantedQos()).willReturn(new int[] {2, 0});
		willReturn(token).given(client).subscribe(any(String[].class), any(int[].class), any());

		MqttPahoMessageDrivenChannelAdapter adapter =
				new MqttPahoMessageDrivenChannelAdapter("tcp://mqtt.host", "bar", factory, "baz", "fix");
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		AtomicReference<Method> method = new AtomicReference<>();
		ReflectionUtils.doWithMethods(MqttPahoMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);
		}, m -> m.getName().equals("connect"));
		assertThat(method.get()).isNotNull();
		method.get().invoke(adapter);
		ReflectionUtils.doWithMethods(MqttPahoMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);
		}, m -> m.getName().equals("subscribe"));
		assertThat(method.get()).isNotNull();
		LogAccessor logger = spy(TestUtils.<LogAccessor>getPropertyValue(adapter, "logger"));
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		given(logger.isWarnEnabled()).willReturn(true);
		method.get().invoke(adapter);
		verify(logger, atLeastOnce())
				.warn(ArgumentMatchers.<Supplier<? extends CharSequence>>argThat(logMessage ->
						logMessage.get()
								.equals("Granted QOS different to Requested QOS; topics: [baz, fix] " +
										"requested: [1, 1] granted: [2, 0]")));

		new DirectFieldAccessor(adapter).setPropertyValue("running", Boolean.TRUE);
		adapter.stop();
		verify(client).disconnectForcibly(30_000L, 5_000L);
	}

	@Test
	public void emptyTopicNotAllowed() {
		assertThatIllegalArgumentException()
				.isThrownBy(() ->
						new MqttPahoMessageDrivenChannelAdapter("client_id", mock(MqttPahoClientFactory.class), ""))
				.withMessage("The topic to subscribe cannot be empty string");

		var adapter = new MqttPahoMessageDrivenChannelAdapter("client_id", mock(MqttPahoClientFactory.class), "topic1");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> adapter.addTopic(""))
				.withMessage("The topic to subscribe cannot be empty string");
	}

	private MqttPahoMessageDrivenChannelAdapter buildAdapterIn(final IMqttAsyncClient client, Boolean cleanSession)
			throws MqttException {

		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory() {

			@Override
			public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) {
				return client;
			}

		};
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[] {"tcp://localhost:1883"});
		if (cleanSession != null) {
			connectOptions.setCleanSession(cleanSession);
		}
		factory.setConnectionOptions(connectOptions);
		given(client.isConnected()).willReturn(true);
		IMqttToken token = mock(IMqttToken.class);
		given(client.connect(any(MqttConnectOptions.class))).willReturn(token);
		given(client.subscribe(any(String[].class), any(int[].class), any())).willReturn(token);
		given(token.getGrantedQos()).willReturn(new int[] {2});
		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("client", factory, "foo");
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.setOutputChannel(new NullChannel());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		return adapter;
	}

	private MqttPahoMessageHandler buildAdapterOut(final IMqttAsyncClient client) {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory() {

			@Override
			public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) {
				return client;
			}

		};
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[] {"tcp://localhost:1883"});
		factory.setConnectionOptions(connectOptions);
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("client", factory);
		adapter.setDefaultTopic("foo");
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		return adapter;
	}

	private void verifyUnsubscribe(IMqttAsyncClient client) throws Exception {
		verify(client).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(any(String[].class), any(int[].class), any());
		verify(client).unsubscribe(any(String[].class));
		verify(client).disconnectForcibly(anyLong(), anyLong());
	}

	private void verifyNotUnsubscribe(IMqttAsyncClient client) throws Exception {
		verify(client).connect(any(MqttConnectOptions.class));
		verify(client).subscribe(any(String[].class), any(int[].class), any());
		verify(client, never()).unsubscribe(any(String[].class));
		verify(client).disconnectForcibly(anyLong(), anyLong());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
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
