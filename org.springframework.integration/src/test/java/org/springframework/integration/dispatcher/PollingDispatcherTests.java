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

package org.springframework.integration.dispatcher;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.scheduling.Schedule;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class PollingDispatcherTests {

	private PollingDispatcher pollingDispatcher;
	private Schedule scheduleMock = createMock(Schedule.class);
	private MessageDispatcher dispatcherMock = createMock(MessageDispatcher.class);
	private BlockingSource sourceMock = createMock(BlockingSource.class);
	private Message messageMock = createMock(Message.class);
	private Object[] globalMocks = new Object[] { scheduleMock, dispatcherMock, sourceMock, messageMock };


	@Before
	public void init() {
		pollingDispatcher = new PollingDispatcher(sourceMock, scheduleMock, dispatcherMock);
		pollingDispatcher.setReceiveTimeout(-1);
		reset(globalMocks);
	}


	@Test
	public void singleMessage() {
		expect(sourceMock.receive()).andReturn(messageMock);
		expect(dispatcherMock.send(messageMock)).andReturn(true);
		replay(globalMocks);
		pollingDispatcher.setMaxMessagesPerPoll(1);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void multipleMessages() {
		expect(sourceMock.receive()).andReturn(messageMock).times(5);
		expect(dispatcherMock.send(messageMock)).andReturn(true).times(5);
		replay(globalMocks);
		pollingDispatcher.setMaxMessagesPerPoll(5);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void multipleMessages_underrun() {
		expect(sourceMock.receive()).andReturn(messageMock).times(5);
		expect(sourceMock.receive()).andReturn(null);
		expect(dispatcherMock.send(messageMock)).andReturn(true).times(5);
		replay(globalMocks);
		pollingDispatcher.setMaxMessagesPerPoll(6);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void droppedMessage() {
		expect(sourceMock.receive()).andReturn(messageMock);
		expect(dispatcherMock.send(messageMock)).andReturn(false);
		replay(globalMocks);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void droppedMessage_onePerPoll() {
		expect(sourceMock.receive()).andReturn(messageMock).times(1);
		expect(dispatcherMock.send(messageMock)).andReturn(false).anyTimes();
		replay(globalMocks);
		pollingDispatcher.setMaxMessagesPerPoll(10);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void blockingSourceTimedOut() {
		pollingDispatcher = new PollingDispatcher(sourceMock, scheduleMock, dispatcherMock);
		// we don't need to await the timeout, returning null suffices
		expect(sourceMock.receive(1)).andReturn(null);
		replay(globalMocks);
		pollingDispatcher.setReceiveTimeout(1);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void blockingSourceNotTimedOut() {
		pollingDispatcher = new PollingDispatcher(sourceMock, scheduleMock, dispatcherMock);
		expect(sourceMock.receive(1)).andReturn(messageMock);
		expect(dispatcherMock.send(messageMock)).andReturn(false);
		replay(globalMocks);
		pollingDispatcher.setReceiveTimeout(1);
		pollingDispatcher.run();
		verify(globalMocks);
	}

}
