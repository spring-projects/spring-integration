/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageMapper;

/**
 * @author Iwein Fuld
 */
public class SimpleMessagingGatewayTests {

	private SimpleMessagingGateway simpleMessagingGateway;

	private MessageChannel requestChannel;

	private Object[] allmocks;

	private Message messageMock;

	private MessageChannel replyChannel;

	private MessageMapper messageMapperMock;


	@Before
	public void initializeSample() {
		this.requestChannel = createMock(MessageChannel.class);
		this.replyChannel = createMock(MessageChannel.class);
		this.messageMock = createMock(Message.class);
		this.messageMapperMock = createMock(MessageMapper.class);

		this.simpleMessagingGateway = new SimpleMessagingGateway(requestChannel);
		this.simpleMessagingGateway.setReplyChannel(replyChannel);
		this.simpleMessagingGateway.setMessageMapper(messageMapperMock);
		this.allmocks = new Object[] { requestChannel, replyChannel, messageMock, messageMapperMock };
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

	//TODO null cases for send

	/* receive tests */

	@Test
	public void receiveMessage() {
		expect(replyChannel.receive()).andReturn(messageMock);
		expect(messageMapperMock.mapMessage(messageMock)).andReturn(messageMock);
		replay(allmocks);
		assertEquals(this.simpleMessagingGateway.receive(), messageMock);
		verify(allmocks);
	}

	@Test
	public void receiveMessage_null() {
		expect(replyChannel.receive()).andReturn(null);
		replay(allmocks);
		assertEquals(this.simpleMessagingGateway.receive(), null);
		verify(allmocks);
	}

}
