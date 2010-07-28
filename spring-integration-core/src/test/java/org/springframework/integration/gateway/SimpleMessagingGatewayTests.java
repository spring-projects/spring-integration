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

import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageDeliveryException;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class SimpleMessagingGatewayTests {

	private SimpleMessagingGateway simpleMessagingGateway;

	private MessageChannel requestChannel = createMock(MessageChannel.class);

	private PollableChannel replyChannel = createMock(PollableChannel.class);

	private Message messageMock = createMock(Message.class);

	private Object[] allmocks = new Object[] { requestChannel, replyChannel, messageMock };


	@Before
	public void initializeSample() {
		this.simpleMessagingGateway = new SimpleMessagingGateway();
		this.simpleMessagingGateway.setRequestChannel(requestChannel);
		this.simpleMessagingGateway.setReplyChannel(replyChannel);
		this.simpleMessagingGateway.setBeanFactory(TestUtils.createTestApplicationContext());
		this.simpleMessagingGateway.afterPropertiesSet();
		this.simpleMessagingGateway.start();
		reset(allmocks);
	}


	/* send tests */

	@Test
	public void sendMessage() {
		expect(requestChannel.send(messageMock)).andReturn(true);
		replay(allmocks);
		this.simpleMessagingGateway.send(messageMock);
		verify(allmocks);
	}

	@Test(expected=MessageDeliveryException.class)
	public void sendMessage_failure() {
		expect(messageMock.getHeaders()).andReturn(new MessageHeaders(null));
		expect(requestChannel.send(messageMock)).andReturn(false);
		replay(allmocks);
		this.simpleMessagingGateway.send(messageMock);
		verify(allmocks);
	}

	@Test
	public void sendObject() {
		expect(requestChannel.send(isA(Message.class))).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				assertEquals("test", ((Message) getCurrentArguments()[0]).getPayload());
				return true;
			}
		});
		replay(allmocks);
		this.simpleMessagingGateway.send("test");
		verify(allmocks);
	}

	@Test(expected=MessageDeliveryException.class)
	public void sendObject_failure() {
		expect(requestChannel.send(isA(Message.class))).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				assertEquals("test", ((Message) getCurrentArguments()[0]).getPayload());
				return false;
			}
		});
		replay(allmocks);
		this.simpleMessagingGateway.send("test");
		verify(allmocks);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendMessage_null() {
		replay(allmocks);
		try {
			this.simpleMessagingGateway.send(null);
		}
		finally {
			verify(allmocks);
		}
	}

	/* receive tests */

	@Test
	public void receiveMessage() {
		expect(replyChannel.receive()).andReturn(messageMock);
		expect(messageMock.getPayload()).andReturn("test").anyTimes();
		replay(allmocks);
		assertEquals("test", this.simpleMessagingGateway.receive());
		verify(allmocks);
	}

	@Test
	public void receiveMessage_null() {
		expect(replyChannel.receive()).andReturn(null);
		replay(allmocks);
		assertNull(this.simpleMessagingGateway.receive());
		verify(allmocks);
	}

	/* send and receive tests */

	@Test
	public void sendObjectAndReceiveObject() {
		expect(replyChannel.receive(100)).andReturn(messageMock);
		expect(requestChannel.send(isA(Message.class))).andReturn(true);
		replay(allmocks);
		// TODO: if timeout is 0, this will fail occasionally
		this.simpleMessagingGateway.setReplyTimeout(100);
		this.simpleMessagingGateway.sendAndReceive("test");
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
		this.simpleMessagingGateway.setReplyTimeout(0);
		this.simpleMessagingGateway.sendAndReceive(messageMock);
		verify(allmocks);
		verify(messageHeadersMock);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendNullAndReceiveObject() {
		replay(allmocks);
		try {
			this.simpleMessagingGateway.sendAndReceive(null);
		}
		finally {
			verify(allmocks);
		}
	}

	@Test
	public void sendObjectAndReceiveMessage() {
		expect(replyChannel.receive(100)).andReturn(messageMock);
		expect(requestChannel.send(isA(Message.class))).andReturn(true);
		replay(allmocks);
		// TODO: commenting the next line causes the test to hang
		this.simpleMessagingGateway.setReplyTimeout(100);
		this.simpleMessagingGateway.sendAndReceiveMessage("test");
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
		this.simpleMessagingGateway.sendAndReceiveMessage(messageMock);
		verify(allmocks);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendNullAndReceiveMessage() {
		replay(allmocks);
		try {
			this.simpleMessagingGateway.sendAndReceiveMessage(null);
		}
		finally {
			verify(allmocks);
		}
	}

}
