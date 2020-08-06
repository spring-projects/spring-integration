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

package org.springframework.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
@SuppressWarnings("unchecked")
public class MessagingGatewayTests {

	private final TestApplicationContext applicationContext = TestUtils.createTestApplicationContext();

	private volatile MessagingGatewaySupport messagingGateway;

	private volatile MessageChannel requestChannel = Mockito.mock(MessageChannel.class);

	private volatile PollableChannel replyChannel = Mockito.mock(PollableChannel.class);

	@SuppressWarnings("rawtypes")
	private volatile Message messageMock = Mockito.mock(Message.class);

	@Before
	public void initializeSample() {
		this.messagingGateway = new MessagingGatewaySupport() {

		};
		this.messagingGateway.setRequestChannel(this.requestChannel);
		this.messagingGateway.setReplyChannel(this.replyChannel);

		this.messagingGateway.setBeanFactory(this.applicationContext);
		this.messagingGateway.afterPropertiesSet();
		this.messagingGateway.start();
		this.applicationContext.refresh();
		Mockito.when(this.messageMock.getHeaders()).thenReturn(new MessageHeaders(Collections.emptyMap()));
	}

	@After
	public void tearDown() {
		this.messagingGateway.stop();
		this.applicationContext.close();
	}

	/* send tests */

	@Test
	public void sendMessage() {
		Mockito.when(requestChannel.send(messageMock, 1000L)).thenReturn(true);
		this.messagingGateway.send(messageMock);
		Mockito.verify(requestChannel).send(messageMock, 1000L);
		// TODO Micrometer counter
//		assertThat(this.messagingGateway.getMessageCount()).isEqualTo(1);
	}

	@Test(expected = MessageDeliveryException.class)
	public void sendMessage_failure() {
		Mockito.when(messageMock.getHeaders()).thenReturn(new MessageHeaders(null));
		Mockito.when(requestChannel.send(messageMock, 1000L)).thenReturn(false);
		this.messagingGateway.send(messageMock);
	}

	@Test
	public void sendObject() {
		Mockito.doAnswer(invocation -> {
			assertThat(((Message<?>) invocation.getArguments()[0]).getPayload()).isEqualTo("test");
			return true;
		}).when(requestChannel).send(Mockito.any(Message.class), Mockito.eq(1000L));

		this.messagingGateway.send("test");
		Mockito.verify(requestChannel).send(Mockito.any(Message.class), Mockito.eq(1000L));
	}

	@Test(expected = MessageDeliveryException.class)
	public void sendObject_failure() {
		Mockito.doAnswer(invocation -> {
			assertThat(((Message<?>) invocation.getArguments()[0]).getPayload()).isEqualTo("test");
			return false;
		}).when(requestChannel).send(Mockito.any(Message.class), Mockito.eq(1000L));

		this.messagingGateway.send("test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendMessage_null() {
		this.messagingGateway.send(null);
	}

	/* receive tests */

	@Test
	public void receiveMessage() {
		Mockito.when(replyChannel.receive(1000L)).thenReturn(messageMock);
		Mockito.when(messageMock.getPayload()).thenReturn("test");
		assertThat(this.messagingGateway.receive()).isEqualTo("test");
		Mockito.verify(replyChannel).receive(1000L);
	}

	@Test
	public void receiveMessage_null() {
		Mockito.when(replyChannel.receive(1000L)).thenReturn(null);
		assertThat(this.messagingGateway.receive()).isNull();
		Mockito.verify(replyChannel).receive(1000L);
	}

	/* send and receive tests */

	@Test
	public void sendObjectAndReceiveObject() {
		Mockito.when(replyChannel.receive(100L)).thenReturn(messageMock);
		Mockito.when(messageMock.getPayload()).thenReturn("test");
		Mockito.doAnswer(invocation -> {
			Message<?> message = (Message<?>) invocation.getArguments()[0];
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
			return true;
		}).when(requestChannel).send(Mockito.any(Message.class), Mockito.anyLong());

		// TODO: if timeout is 0, this will fail occasionally
		this.messagingGateway.setReplyTimeout(100);
		Object test = this.messagingGateway.sendAndReceive("test");
		assertThat(test).isEqualTo("test");
	}

	@Test
	public void sendMessageAndReceiveObject() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(MessageHeaders.ID, UUID.randomUUID());
		MessageHeaders messageHeadersMock = new MessageHeaders(headers);
		Mockito.when(replyChannel.receive(0)).thenReturn(messageMock);
		Mockito.when(messageMock.getHeaders()).thenReturn(messageHeadersMock);
		Mockito.when(messageMock.getPayload()).thenReturn("foo");

		Mockito.doAnswer(invocation -> {
			Message<?> message = (Message<?>) invocation.getArguments()[0];
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
			return true;
		}).when(requestChannel).send(Mockito.any(Message.class), Mockito.anyLong());

		this.messagingGateway.setReplyTimeout(0);
		Object o = this.messagingGateway.sendAndReceive(messageMock);
		assertThat(o).isEqualTo("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendNullAndReceiveObject() {
		this.messagingGateway.sendAndReceive(null);
	}

	@Test
	public void sendObjectAndReceiveMessage() {
		Mockito.when(messageMock.getPayload()).thenReturn("foo");
		Mockito.when(replyChannel.receive(100L)).thenReturn(messageMock);
		Mockito.doAnswer(invocation -> {
			Message<?> message = (Message<?>) invocation.getArguments()[0];
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(messageMock);
			return true;
		}).when(requestChannel).send(Mockito.any(Message.class), Mockito.anyLong());

		this.messagingGateway.setReplyTimeout(100L);
		Message<?> receiveMessage = this.messagingGateway.sendAndReceiveMessage("test");
		assertThat(receiveMessage).isSameAs(messageMock);
	}

	@Test
	public void sendMessageAndReceiveMessage() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(MessageHeaders.ID, UUID.randomUUID());
		headers.put(MessageHeaders.REPLY_CHANNEL, replyChannel);
		MessageHeaders messageHeadersMock = new MessageHeaders(headers);
		Mockito.when(replyChannel.receive(Mockito.anyLong())).thenReturn(messageMock);
		Mockito.when(messageMock.getHeaders()).thenReturn(messageHeadersMock);
		Mockito.when(messageMock.getPayload()).thenReturn("foo");
		Mockito.doAnswer(invocation -> {
			Message<?> message = (Message<?>) invocation.getArguments()[0];
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(messageMock);
			return true;
		}).when(requestChannel).send(Mockito.any(Message.class), Mockito.anyLong());

		Message<?> receiveMessage = this.messagingGateway.sendAndReceiveMessage(messageMock);
		assertThat(receiveMessage).isSameAs(messageMock);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendNullAndReceiveMessage() {
		this.messagingGateway.sendAndReceiveMessage(null);
	}

	// should fail but it doesn't now
	@Test(expected = MessagingException.class)
	public void validateErrorMessageCanNotBeReplyMessage() {
		DirectChannel reqChannel = new DirectChannel();
		reqChannel.subscribe(message -> {
			throw new RuntimeException("ooops");
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new MyErrorService());
		handler.setBeanFactory(this.applicationContext);
		handler.afterPropertiesSet();
		errorChannel.subscribe(handler);

		this.messagingGateway = new MessagingGatewaySupport() {

		};

		this.messagingGateway.setRequestChannel(reqChannel);
		this.messagingGateway.setErrorChannel(errorChannel);
		this.messagingGateway.setReplyChannel(null);
		this.messagingGateway.setBeanFactory(this.applicationContext);
		this.messagingGateway.afterPropertiesSet();
		this.messagingGateway.start();

		this.messagingGateway.sendAndReceiveMessage("hello");
	}

	@Test
	public void validateErrorChannelWithSuccessfulReply() throws InterruptedException {
		DirectChannel reqChannel = new DirectChannel();
		reqChannel.subscribe(message -> {
			throw new RuntimeException("ooops");
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		MyOneWayErrorService myOneWayErrorService = new MyOneWayErrorService();
		ServiceActivatingHandler handler = new ServiceActivatingHandler(myOneWayErrorService);
		handler.setBeanFactory(this.applicationContext);
		handler.afterPropertiesSet();
		errorChannel.subscribe(handler);

		this.messagingGateway = new MessagingGatewaySupport() {

		};

		this.messagingGateway.setRequestChannel(reqChannel);
		this.messagingGateway.setErrorChannel(errorChannel);
		this.messagingGateway.setReplyChannel(null);
		this.messagingGateway.setBeanFactory(mock(BeanFactory.class));
		this.messagingGateway.afterPropertiesSet();
		this.messagingGateway.start();

		this.messagingGateway.send("hello");

		assertThat(myOneWayErrorService.errorReceived.await(10, TimeUnit.SECONDS)).isTrue();
	}

	public static class MyErrorService {

		public Message<?> handleErrorMessage(Message<?> errorMessage) {
			return errorMessage;
		}

	}

	public static class MyOneWayErrorService {

		private final CountDownLatch errorReceived = new CountDownLatch(1);

		public void handleErrorMessage(Message<?> errorMessage) {
			this.errorReceived.countDown();
		}

	}

}
