package org.springframework.integration.dispatcher;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.scheduling.Schedule;

/**
 * 
 * @author Iwein Fuld
 * 
 */
@SuppressWarnings("unchecked")
public class PollingDispatcherTest {

	private PollingDispatcher pollingDispatcher;
	private Schedule scheduleMock = createMock(Schedule.class);
	private MessageDispatcher dispatcherMock = createMock(MessageDispatcher.class);
	private BlockingSource sourceMock = createMock(BlockingSource.class);
	private Message messageMock = createMock(Message.class);
	private Object[] globalMocks = new Object[] { scheduleMock, dispatcherMock,
			sourceMock, messageMock };

	@Before
	public void init() {
		pollingDispatcher = new PollingDispatcher(sourceMock, dispatcherMock,
				scheduleMock);
		pollingDispatcher.setReceiveTimeout(-1);
		reset(globalMocks);
	}

	@Test
	public void singleMessage() {
		expect(sourceMock.receive()).andReturn(messageMock);
		expect(dispatcherMock.send(messageMock)).andReturn(true);
		replay(globalMocks);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void multipleMessages() {
		expect(sourceMock.receive()).andReturn(messageMock).times(5);
		expect(dispatcherMock.send(messageMock)).andReturn(true).times(5);
		replay(globalMocks);
		pollingDispatcher.setMaxMessagesPerTask(5);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void multipleMessages_underrun() {
		expect(sourceMock.receive()).andReturn(messageMock).times(5);
		expect(sourceMock.receive()).andReturn(null);
		expect(dispatcherMock.send(messageMock)).andReturn(true).times(5);
		replay(globalMocks);
		pollingDispatcher.setMaxMessagesPerTask(6);
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
		pollingDispatcher.setMaxMessagesPerTask(10);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void blockingSourceTimedOut() {
		pollingDispatcher = new PollingDispatcher(sourceMock, dispatcherMock,
				scheduleMock);
		// we don't need to await the timeout, returning null suffices
		expect(sourceMock.receive(1)).andReturn(null);
		replay(globalMocks);
		pollingDispatcher.setReceiveTimeout(1);
		pollingDispatcher.run();
		verify(globalMocks);
	}

	@Test
	public void blockingSourceNotTimedOut() {
		pollingDispatcher = new PollingDispatcher(sourceMock, dispatcherMock,
				scheduleMock);
		expect(sourceMock.receive(1)).andReturn(messageMock);
		expect(dispatcherMock.send(messageMock)).andReturn(false);
		replay(globalMocks);
		pollingDispatcher.setReceiveTimeout(1);
		pollingDispatcher.run();
		verify(globalMocks);
	}
}
