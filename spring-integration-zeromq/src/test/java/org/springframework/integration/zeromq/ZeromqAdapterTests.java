/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.zeromq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.zeromq.SocketType;
import org.zeromq.ZAuth;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zeromq.core.ConsumerStopAction;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils;

/**
 * @author Subhobrata Dey
 *
 * @since 5.1
 *
 */
public class ZeromqAdapterTests {

	@Test
	public void testZmqConnectOptions() {
		org.springframework.integration.zeromq.core.DefaultZeromqClientFactory factory = new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory();
		factory.setCleanSession(false);
		factory.setPassword("password");
		factory.setUserName("username");
		factory.setClientType(SocketType.PUB.type());
		factory.setServerURI("tcp://*:5556");
		factory.setConsumerStopAction(ConsumerStopAction.UNSUBSCRIBE_ALWAYS);

		assertThat(factory.cleanSession()).isFalse();
		assertThat("password").isEqualTo(factory.getPassword());
		assertThat("username").isEqualTo(factory.getUserName());
		assertThat("tcp://*:5556").isEqualTo(factory.getServerURI());
		assertThat(ConsumerStopAction.UNSUBSCRIBE_ALWAYS).isEqualTo(factory.getConsumerStopAction());
	}

	@Test
	public void testOutboundOptionsApplied() throws Exception {
		org.springframework.integration.zeromq.core.DefaultZeromqClientFactory factory = new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory();
		factory.setCleanSession(false);
		factory.setPassword("password");
		factory.setUserName("username");
		factory.setClientType(SocketType.PUB.type());
		factory.setServerURI("tcp://*:5556");

		factory = spy(factory);
		final ZContext zContext = mock(ZContext.class);
		willAnswer(invocation -> zContext).given(factory).getZContext();
		final ZAuth zAuth = mock(ZAuth.class);
		willAnswer(invocation -> zAuth).given(factory).getZAuth();
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		willAnswer(invocation -> client).given(factory).getClientInstance(anyString(), any());
		final ZMQ.Poller poller = mock(ZMQ.Poller.class);
		willAnswer(invocation -> poller).given(factory).getPollerInstance(anyInt());
		willAnswer(invocation -> true).given(poller).pollout(anyInt());

		org.springframework.integration.zeromq.outbound.ZeromqMessageHandler handler = new org.springframework.integration.zeromq.outbound.ZeromqMessageHandler("tcp://*:5556", "bar", factory);
		handler.setTopic("zmq-foo");
		handler.setConverter(new org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter());

		final AtomicBoolean connectCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			String serverUri = invocation.getArgument(0);
			assertThat("tcp://*:5556").isEqualTo(serverUri);
			connectCalled.set(true);
			return true;
		}).given(client).bind(anyString());

		final AtomicBoolean publishCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			byte[] messagePayload = invocation.getArgument(0);
			assertThat("zmq-foo Hello, world!").isEqualTo(new String(messagePayload));
			publishCalled.set(true);
			return true;
		}).given(client).send(any(byte[].class), anyInt());

		handler.start();
		handler.publish("zmq-foo",
				"Hello, world!".getBytes(Charset.defaultCharset()),
				new GenericMessage<>("Hello, world!"));

		while (!publishCalled.get()) {
			Thread.sleep(10);
		}

		verify(client, times(1)).bind(anyString());
		assertThat(connectCalled.get()).isTrue();
	}

	@Test
	public void testInboundOptionsApplied() throws Exception {
		org.springframework.integration.zeromq.core.DefaultZeromqClientFactory factory = new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory();
		factory.setCleanSession(false);
		factory.setPassword("password");
		factory.setUserName("username");
		factory.setClientType(SocketType.SUB.type());
		factory.setServerURI("tcp://localhost:5556");

		factory = spy(factory);
		final ZMQ.Context context = mock(ZMQ.Context.class);
		willAnswer(invocation -> context).given(factory).getContext();
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		willAnswer(invocation -> client).given(factory).getClientInstance(anyString(), anyString());
		final ZMQ.Poller poller = mock(ZMQ.Poller.class);
		willAnswer(invocation -> poller).given(factory).getPollerInstance(anyInt());
		willAnswer(invocation -> true).given(poller).pollin(anyInt());

		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter handler =
				new org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter("tcp://localhost:5556", "bar", factory);
		handler.setTopic("zmq-foo");
		handler.setConverter(new org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter());
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		handler.setTaskScheduler(taskScheduler);
		handler.setBeanFactory(mock(BeanFactory.class));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		final BlockingQueue<org.springframework.integration.zeromq.event.ZeromqIntegrationEvent> events = new LinkedBlockingQueue<>();
		willAnswer(invocation -> {
			events.add(invocation.getArgument(0));
			return null;
		}).given(applicationEventPublisher).publishEvent(any(org.springframework.integration.zeromq.event.ZeromqIntegrationEvent.class));
		handler.setApplicationEventPublisher(applicationEventPublisher);

		final AtomicBoolean connectCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			String serverUri = invocation.getArgument(0);
			assertThat("tcp://localhost:5556").isEqualTo(serverUri);
			connectCalled.set(true);
			return true;
		}).given(client).connect(anyString());

		final AtomicBoolean subscribeCalled = new AtomicBoolean();
		willAnswer(invocation -> {
			subscribeCalled.set(true);
			return "zmq-foo Hello, world!".getBytes(Charset.defaultCharset());
		}).given(client).recv(anyInt());

		handler.start();

		verify(client, times(1)).connect(anyString());
		assertThat(connectCalled.get()).isTrue();

		int n = 0;
		org.springframework.integration.zeromq.event.ZeromqIntegrationEvent event = events.poll(10, TimeUnit.SECONDS);
		while (!(event instanceof org.springframework.integration.zeromq.event.ZeromqSubscribedEvent && n++ < 20)) {
			event = events.poll(10, TimeUnit.SECONDS);
		}
		assertThat(event).isInstanceOf(org.springframework.integration.zeromq.event.ZeromqSubscribedEvent.class);
		assertThat("Connected and subscribed to zmq-foo").isEqualTo(((org.springframework.integration.zeromq.event.ZeromqSubscribedEvent) event).getMessage());

		taskScheduler.destroy();
	}

	@Test
	public void testStopActionDefault() {
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter = buildAdapter(client, true, null);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);
	}

	@Test
	public void testStopActionDefaultNotClean() {
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter = buildAdapter(client, false, null);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@Test
	public void testStopActionAlways() {
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter =
				buildAdapter(client, false, ConsumerStopAction.UNSUBSCRIBE_ALWAYS);

		adapter.start();
		adapter.stop();
		verifyUnsubscribe(client);

		TaskScheduler taskScheduler = TestUtils.getPropertyValue(adapter, "taskScheduler", TaskScheduler.class);

		verify(taskScheduler, never()).schedule(any(Runnable.class), any(Date.class));
	}

	@Test
	public void testUnsubscribeNever() {
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter =
				buildAdapter(client, false, ConsumerStopAction.UNSUBSCRIBE_NEVER);

		adapter.start();
		adapter.stop();
		verifyNotUnsubscribe(client);
	}

	@Test
	public void testReconnect() throws Exception {
		final ZMQ.Socket client = mock(ZMQ.Socket.class);
		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter =
				buildAdapter(client, false, ConsumerStopAction.UNSUBSCRIBE_NEVER);
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
		Thread.sleep(1000);
		assertThat(attemptingReconnectCount.get()).isLessThanOrEqualTo(2);
		adapter.stop();
		taskScheduler.destroy();
	}

	@Test
	public void testSubscribeFailure() throws Exception {
		org.springframework.integration.zeromq.core.DefaultZeromqClientFactory factory = new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory();
		factory.setCleanSession(false);
		factory.setPassword("password");
		factory.setUserName("username");
		factory.setClientType(SocketType.SUB.type());

		factory = spy(factory);
		ZMQ.Socket client = mock(ZMQ.Socket.class);
		willAnswer(invocation -> client).given(factory).getClientInstance(anyString(), anyString());
		willAnswer(new CallsRealMethods()).given(client).connect(anyString());

		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter = new org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter("foo", "bar", factory);
		adapter.setTopic("zmq-foo");
		AtomicReference<Method> method = new AtomicReference<>();
		ReflectionUtils.doWithMethods(org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter.class, m -> {
			m.setAccessible(true);
			method.set(m);

		}, m -> m.getName().equals("connectAndSubscribe"));
		assertThat(method.get()).isNotNull();
		try {
			method.get().invoke(adapter);
			fail("Expected InvocationTargetException");
		}
		catch (InvocationTargetException e) {
			assertThat(e.getCause()).isInstanceOf(NullPointerException.class);
		}
	}


	private org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter buildAdapter(final ZMQ.Socket client, Boolean cleanSession, ConsumerStopAction consumerStopAction) throws ZMQException {
		org.springframework.integration.zeromq.core.DefaultZeromqClientFactory factory = new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory() {
			@Override
			public ZMQ.Socket getClientInstance(String clientId, String... topic) {
				return client;
			}

			@Override
			public ZMQ.Poller getPollerInstance(int pollerType) {
				return mock(ZMQ.Poller.class);
			}
		};
		factory.setServerURI("tcp://localhost:5556");
		if (cleanSession != null) {
			factory.setCleanSession(cleanSession);
		}
		if (consumerStopAction != null) {
			factory.setConsumerStopAction(consumerStopAction);
		}
		org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter adapter = new org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter("client", factory);
		adapter.setTopic("zmq-foo");
		adapter.setOutputChannel(new NullChannel());
		adapter.setTaskScheduler(mock(TaskScheduler.class));
		return adapter;
	}

	private void verifyUnsubscribe(ZMQ.Socket client) {
		verify(client).connect(anyString());
		verify(client).unsubscribe(any(byte[].class));
		verify(client).disconnect(anyString());
		verify(client).close();
	}

	private void verifyNotUnsubscribe(ZMQ.Socket client) {
		verify(client).connect(anyString());
		verify(client, never()).unsubscribe(any(byte[].class));
		verify(client).disconnect(anyString());
		verify(client).close();
	}
}
