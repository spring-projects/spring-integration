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

package org.springframework.integration.endpoint;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.scheduling.Trigger;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class ChannelPollerTests {

	private ChannelPoller poller;
	private Trigger triggerMock = createMock(Trigger.class);
	private PollableChannel channelMock = createMock(PollableChannel.class);
	private MessageConsumer endpointMock = createMock(MessageConsumer.class);
	private Message messageMock = createMock(Message.class);
	private Object[] globalMocks = new Object[] { triggerMock, channelMock, endpointMock, messageMock };


	@Before
	public void init() {
		poller = new ChannelPoller(channelMock, triggerMock);
		poller.setConsumer(endpointMock);
		poller.setReceiveTimeout(-1);
		reset(globalMocks);
	}


	@Test
	public void singleMessage() {
		expect(channelMock.receive()).andReturn(messageMock);
		endpointMock.onMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		poller.setMaxMessagesPerPoll(1);
		poller.run();
		verify(globalMocks);
	}

	@Test
	public void multipleMessages() {
		expect(channelMock.receive()).andReturn(messageMock).times(5);
		endpointMock.onMessage(messageMock);
		expectLastCall().times(5);
		replay(globalMocks);
		poller.setMaxMessagesPerPoll(5);
		poller.run();
		verify(globalMocks);
	}

	@Test
	public void multipleMessages_underrun() {
		expect(channelMock.receive()).andReturn(messageMock).times(5);
		expect(channelMock.receive()).andReturn(null);
		endpointMock.onMessage(messageMock);
		expectLastCall().times(5);
		replay(globalMocks);
		poller.setMaxMessagesPerPoll(6);
		poller.run();
		verify(globalMocks);
	}

	@Test(expected = MessageRejectedException.class)
	public void rejectedMessage() {
		expect(channelMock.receive()).andReturn(messageMock);
		endpointMock.onMessage(messageMock);
		expectLastCall().andThrow(new MessageRejectedException(messageMock, "intentional test failure"));
		replay(globalMocks);
		poller.run();
		verify(globalMocks);
	}

	@Test(expected = MessageRejectedException.class)
	public void droppedMessage_onePerPoll() {
		expect(channelMock.receive()).andReturn(messageMock).times(1);
		endpointMock.onMessage(messageMock);
		expectLastCall().andThrow(new MessageRejectedException(messageMock, "intentional test failure")).anyTimes();
		replay(globalMocks);
		poller.setMaxMessagesPerPoll(10);
		poller.run();
		verify(globalMocks);
	}

	@Test
	public void blockingSourceTimedOut() {
		poller = new ChannelPoller(channelMock, triggerMock);
		poller.setConsumer(endpointMock);
		// we don't need to await the timeout, returning null suffices
		expect(channelMock.receive(1)).andReturn(null);
		replay(globalMocks);
		poller.setReceiveTimeout(1);
		poller.run();
		verify(globalMocks);
	}

	@Test
	public void blockingSourceNotTimedOut() {
		poller = new ChannelPoller(channelMock, triggerMock);
		poller.setConsumer(endpointMock);
		expect(channelMock.receive(1)).andReturn(messageMock);
		endpointMock.onMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		poller.setReceiveTimeout(1);
		poller.setMaxMessagesPerPoll(1);
		poller.run();
		verify(globalMocks);
	}

}
