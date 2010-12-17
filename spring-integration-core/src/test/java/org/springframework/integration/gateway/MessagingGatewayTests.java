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

package org.springframework.integration.gateway;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class MessagingGatewayTests {

	private volatile MessagingGatewaySupport messagingGateway;

	private volatile MessageChannel requestChannel = createMock(MessageChannel.class);

	private volatile PollableChannel replyChannel = createMock(PollableChannel.class);

	@SuppressWarnings("rawtypes")
	private volatile Message messageMock = createMock(Message.class);

	private  final Object[] allmocks = new Object[] { requestChannel, replyChannel, messageMock };


	@Before
	public void initializeSample() {
		this.messagingGateway = new MessagingGatewaySupport() {};
		this.messagingGateway.setRequestChannel(requestChannel);
		this.messagingGateway.setReplyChannel(replyChannel);
		this.messagingGateway.setBeanFactory(TestUtils.createTestApplicationContext());
		this.messagingGateway.afterPropertiesSet();
		this.messagingGateway.start();
		reset(allmocks);
	}


	/* send tests */

	@Test
	public void sendMessage() {
		expect(requestChannel.send(messageMock, 1000L)).andReturn(true);
		replay(allmocks);
		this.messagingGateway.send(messageMock);
		verify(allmocks);
	}

	@Test(expected=MessageDeliveryException.class)
	public void sendMessage_failure() {
		expect(messageMock.getHeaders()).andReturn(new MessageHeaders(null));
		expect(requestChannel.send(messageMock, 1000)).andReturn(false);
		replay(allmocks);
		this.messagingGateway.send(messageMock);
		verify(allmocks);
	}

	@Test
	public void sendObject() {
		expect(requestChannel.send(isA(Message.class), eq(1000L))).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				assertEquals("test", ((Message<?>) getCurrentArguments()[0]).getPayload());
				return true;
			}
		});
		replay(allmocks);
		this.messagingGateway.send("test");
		verify(allmocks);
	}

	@Test(expected=MessageDeliveryException.class)
	public void sendObject_failure() {
		expect(requestChannel.send(isA(Message.class), eq(1000L))).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				assertEquals("test", ((Message<?>) getCurrentArguments()[0]).getPayload());
				return false;
			}
		});
		replay(allmocks);
		this.messagingGateway.send("test");
		verify(allmocks);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendMessage_null() {
		replay(allmocks);
		try {
			this.messagingGateway.send(null);
		}
		finally {
			verify(allmocks);
		}
	}

	/* receive tests */

	@Test
	public void receiveMessage() {
		expect(replyChannel.receive(1000)).andReturn(messageMock);
		expect(messageMock.getPayload()).andReturn("test").anyTimes();
		replay(allmocks);
		assertEquals("test", this.messagingGateway.receive());
		verify(allmocks);
	}

	@Test
	public void receiveMessage_null() {
		expect(replyChannel.receive(1000)).andReturn(null);
		replay(allmocks);
		assertNull(this.messagingGateway.receive());
		verify(allmocks);
	}

	/* send and receive tests */

	@Test
	public void sendObjectAndReceiveObject() {
		expect(replyChannel.receive(100)).andReturn(messageMock);
		expect(requestChannel.send(isA(Message.class), eq(1000L))).andReturn(true);
		replay(allmocks);
		// TODO: if timeout is 0, this will fail occasionally
		this.messagingGateway.setReplyTimeout(100);
		this.messagingGateway.sendAndReceive("test");
		verify(allmocks);
	}

	@Test
	@Ignore
	public void sendMessageAndReceiveObject() {
		// setup local mocks
		MessageHeaders messageHeadersMock = createMock(MessageHeaders.class);	
		//set expectations
		expect(replyChannel.receive(0)).andReturn(messageMock);
		expect(messageMock.getHeaders()).andReturn(messageHeadersMock);
		expect(requestChannel.send(messageMock)).andReturn(true);
		expect(messageHeadersMock.getId()).andReturn(UUID.randomUUID());

		//play scenario
		replay(allmocks);
		replay(messageHeadersMock);
		this.messagingGateway.setReplyTimeout(0);
		this.messagingGateway.sendAndReceive(messageMock);
		verify(allmocks);
		verify(messageHeadersMock);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendNullAndReceiveObject() {
		replay(allmocks);
		try {
			this.messagingGateway.sendAndReceive(null);
		}
		finally {
			verify(allmocks);
		}
	}

	@Test
	public void sendObjectAndReceiveMessage() {
		expect(replyChannel.receive(100)).andReturn(messageMock);
		expect(requestChannel.send(isA(Message.class), eq(1000L))).andReturn(true);
		replay(allmocks);
		// TODO: commenting the next line causes the test to hang
		this.messagingGateway.setReplyTimeout(100);
		this.messagingGateway.sendAndReceiveMessage("test");
		verify(allmocks);
	}

	@Test
	@Ignore
	public void sendMessageAndReceiveMessage() {
		// setup local mocks
		MessageHeaders messageHeadersMock = createMock(MessageHeaders.class);	
		//set expectations
		expect(replyChannel.receive(0)).andReturn(messageMock);
		expect(messageMock.getHeaders()).andReturn(messageHeadersMock);
		expect(messageHeadersMock.getReplyChannel()).andReturn(replyChannel);
		expect(requestChannel.send(messageMock)).andReturn(true);
		expect(messageHeadersMock.getId()).andReturn(UUID.randomUUID());

		replay(allmocks);
		this.messagingGateway.sendAndReceiveMessage(messageMock);
		verify(allmocks);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendNullAndReceiveMessage() {
		replay(allmocks);
		try {
			this.messagingGateway.sendAndReceiveMessage(null);
		}
		finally {
			verify(allmocks);
		}
	}
	
	// should fail but it doesn't now
	@Test(expected=MessagingException.class)
	public void validateErroMessageCanNotBeReplyMessage() {
		DirectChannel reqChannel = new DirectChannel();
		reqChannel.subscribe(new MessageHandler() {		
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("ooops");
			}
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		ServiceActivatingHandler handler  = new ServiceActivatingHandler(new MyErrorService());
		handler.afterPropertiesSet();
		errorChannel.subscribe(handler);
		this.messagingGateway = new MessagingGatewaySupport() {};
	
		this.messagingGateway.setRequestChannel(reqChannel);
		this.messagingGateway.setErrorChannel(errorChannel);
		this.messagingGateway.setBeanFactory(TestUtils.createTestApplicationContext());
		this.messagingGateway.afterPropertiesSet();
		this.messagingGateway.start();
		
		this.messagingGateway.sendAndReceiveMessage("hello");
	}
	
	// should not fail but it does now
	@Test
	public void validateErrorChannelWithSuccessfulReply() {
		DirectChannel reqChannel = new DirectChannel();
		reqChannel.subscribe(new MessageHandler() {		
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("ooops");
			}
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		ServiceActivatingHandler handler  = new ServiceActivatingHandler(new MyOneWayErrorService());
		handler.afterPropertiesSet();
		errorChannel.subscribe(handler);
		this.messagingGateway = new MessagingGatewaySupport() {};
	
		this.messagingGateway.setRequestChannel(reqChannel);
		this.messagingGateway.setErrorChannel(errorChannel);
		this.messagingGateway.setBeanFactory(TestUtils.createTestApplicationContext());
		this.messagingGateway.afterPropertiesSet();
		this.messagingGateway.start();
		
		this.messagingGateway.send("hello");		
	}
	
	public static class MyErrorService {
		public Message<?> handleErrorMessage(Message<?> errorMessage){
			return errorMessage;
		}
	}
	
	public static class MyOneWayErrorService {
		public void handleErrorMessage(Message<?> errorMessage){
			return;
		}
	}

}
