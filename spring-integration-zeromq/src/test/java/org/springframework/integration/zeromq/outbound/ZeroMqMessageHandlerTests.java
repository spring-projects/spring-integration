/*
 * Copyright 2020-2024 the original author or authors.
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

package org.springframework.integration.zeromq.outbound;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 * @author Alessio Matricardi
 *
 * @since 5.4
 */
public class ZeroMqMessageHandlerTests {

	private static final ZContext CONTEXT = new ZContext();

	@AfterAll
	static void tearDown() {
		CONTEXT.close();
	}

	@Test
	void testMessageHandlerForPair() {
		String socketAddress = "inproc://messageHandler.test";
		ZMQ.Socket socket = CONTEXT.createSocket(SocketType.PAIR);
		socket.bind(socketAddress);

		ZeroMqMessageHandler messageHandler = new ZeroMqMessageHandler(CONTEXT, socketAddress);
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.setSocketConfigurer(s -> s.setZapDomain("global"));
		messageHandler.afterPropertiesSet();
		messageHandler.start();

		@SuppressWarnings("unchecked")
		Mono<ZMQ.Socket> socketMono = TestUtils.getPropertyValue(messageHandler, "socketMono", Mono.class);
		ZMQ.Socket socketInUse = socketMono.block(Duration.ofSeconds(10));
		assertThat(socketInUse.getZapDomain()).isEqualTo("global");

		Message<?> testMessage = new GenericMessage<>("test");
		messageHandler.handleMessage(testMessage).subscribe();

		assertThat(socket.recvStr()).isEqualTo("test");

		messageHandler.handleMessage(new GenericMessage<>(ZMsg.newStringMsg("test2"))).subscribe();

		assertThat(socket.recvStr()).isEqualTo("test2");

		messageHandler.destroy();
		socket.close();
	}

	@Test
	void testMessageHandlerForPubSub() {
		ZMQ.Socket subSocket = CONTEXT.createSocket(SocketType.SUB);
		subSocket.setReceiveTimeOut(0);
		int port = subSocket.bindToRandomPort("tcp://*");
		subSocket.subscribe("test");

		ZeroMqMessageHandler messageHandler =
				new ZeroMqMessageHandler(CONTEXT, "tcp://localhost:" + port, SocketType.PUB);
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.setTopicExpression(
				new FunctionExpression<Message<?>>((message) -> message.getHeaders().get("topic")));
		messageHandler.setMessageMapper(new EmbeddedJsonHeadersMessageMapper());
		messageHandler.afterPropertiesSet();
		messageHandler.start();

		Message<?> testMessage = MessageBuilder.withPayload("test").setHeader("topic", "testTopic").build();

		await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
				.untilAsserted(() -> {
					subSocket.subscribe("test");
					messageHandler.handleMessage(testMessage).subscribe();
					ZMsg msg = ZMsg.recvMsg(subSocket);
					assertThat(msg).isNotNull();
					assertThat(msg.unwrap().getString(ZMQ.CHARSET)).isEqualTo("testTopic");
					Message<?> capturedMessage =
							new EmbeddedJsonHeadersMessageMapper().toMessage(msg.getFirst().getData());
					assertThat(capturedMessage).isEqualTo(testMessage);
					msg.destroy();
				});

		messageHandler.destroy();
		subSocket.close();
	}

	@Test
	void testMessageHandlerForPushPullOverProxy() {
		ZeroMqProxy proxy = new ZeroMqProxy(CONTEXT);
		proxy.setBeanName("pullPushProxy");
		proxy.afterPropertiesSet();
		proxy.start();

		await().until(() -> proxy.getBackendPort() > 0);

		ZMQ.Socket pullSocket = CONTEXT.createSocket(SocketType.PULL);
		pullSocket.setReceiveTimeOut(20_000);
		pullSocket.connect("tcp://localhost:" + proxy.getBackendPort());

		ZeroMqMessageHandler messageHandler =
				new ZeroMqMessageHandler(CONTEXT, "tcp://localhost:" + proxy.getFrontendPort(), SocketType.PUSH);
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.setMessageConverter(new ByteArrayMessageConverter());
		messageHandler.afterPropertiesSet();
		messageHandler.start();

		Message<?> testMessage = new GenericMessage<>("test".getBytes());
		messageHandler.handleMessage(testMessage).subscribe();

		assertThat(pullSocket.recvStr()).isEqualTo("test");

		messageHandler.destroy();
		pullSocket.close();
		proxy.stop();
		proxy.destroy();
	}

	@Test
	void testMessageHandlerForPubSubDisabledWrapTopic() {
		ZMQ.Socket subSocket = CONTEXT.createSocket(SocketType.SUB);
		subSocket.setReceiveTimeOut(0);
		int port = subSocket.bindToRandomPort("tcp://*");
		subSocket.subscribe("test");

		ZeroMqMessageHandler messageHandler =
				new ZeroMqMessageHandler(CONTEXT, "tcp://localhost:" + port, SocketType.PUB);
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.setTopicExpression(
				new FunctionExpression<Message<?>>((message) -> message.getHeaders().get("topic")));
		messageHandler.setMessageMapper(new EmbeddedJsonHeadersMessageMapper());
		messageHandler.wrapTopic(false);
		messageHandler.afterPropertiesSet();
		messageHandler.start();

		Message<?> testMessage = MessageBuilder.withPayload("test").setHeader("topic", "testTopic").build();

		await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
				.untilAsserted(() -> {
					subSocket.subscribe("test");
					messageHandler.handleMessage(testMessage).subscribe();
					ZMsg msg = ZMsg.recvMsg(subSocket);
					assertThat(msg).isNotNull();
					assertThat(msg.pop().getString(ZMQ.CHARSET)).isEqualTo("testTopic");
					Message<?> capturedMessage =
							new EmbeddedJsonHeadersMessageMapper().toMessage(msg.getFirst().getData());
					assertThat(capturedMessage).isEqualTo(testMessage);
					msg.destroy();
				});

		messageHandler.destroy();
		subSocket.close();
	}

	@Test
	void testMessageHandlerForPubSubWithBind() {
		int boundPort = TestSocketUtils.findAvailableTcpPort();
		ZeroMqMessageHandler messageHandler =
				new ZeroMqMessageHandler(CONTEXT, boundPort, SocketType.PUB);
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.setTopicExpression(
				new FunctionExpression<Message<?>>((message) -> message.getHeaders().get("topic")));
		messageHandler.setMessageMapper(new EmbeddedJsonHeadersMessageMapper());
		messageHandler.wrapTopic(false);
		messageHandler.afterPropertiesSet();
		messageHandler.start();

		ZMQ.Socket subSocket = CONTEXT.createSocket(SocketType.SUB);
		subSocket.setReceiveTimeOut(0);
		subSocket.connect("tcp://localhost:" + boundPort);
		subSocket.subscribe("test");

		Message<?> testMessage = MessageBuilder.withPayload("test").setHeader("topic", "testTopic").build();

		await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
				.untilAsserted(() -> {
					subSocket.subscribe("test");
					messageHandler.handleMessage(testMessage).subscribe();
					ZMsg msg = ZMsg.recvMsg(subSocket);
					assertThat(msg).isNotNull();
					assertThat(msg.pop().getString(ZMQ.CHARSET)).isEqualTo("testTopic");
					Message<?> capturedMessage =
							new EmbeddedJsonHeadersMessageMapper().toMessage(msg.getFirst().getData());
					assertThat(capturedMessage).isEqualTo(testMessage);
					msg.destroy();
				});

		messageHandler.destroy();
		subSocket.close();
	}

}
