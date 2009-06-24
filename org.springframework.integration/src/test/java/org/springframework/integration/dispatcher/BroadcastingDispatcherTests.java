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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class BroadcastingDispatcherTests {

	private BroadcastingDispatcher dispatcher;

	private TaskExecutor taskExecutorMock = createMock(TaskExecutor.class);

	private Message<?> messageMock = createMock(Message.class);

	private MessageHandler targetMock1 = createMock(MessageHandler.class);

	private MessageHandler targetMock2 = createMock(MessageHandler.class);

	private MessageHandler targetMock3 = createMock(MessageHandler.class);

	private Object[] globalMocks = new Object[] {
			messageMock, taskExecutorMock, targetMock1, targetMock2, targetMock3 };


	@Before
	public void init() {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.setTaskExecutor(taskExecutorMock);
		reset(globalMocks);
		defaultTaskExecutorMock();
	}


	@Test
	public void singleTargetWithoutTaskExecutor() throws Exception {
		dispatcher.setTaskExecutor(null);
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void singleTargetWithTaskExecutor() throws Exception {
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsWithoutTaskExecutor() {
		dispatcher.setTaskExecutor(null);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		targetMock2.handleMessage(messageMock);
		expectLastCall();
		targetMock3.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsWithTaskExecutor() {
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		targetMock2.handleMessage(messageMock);
		expectLastCall();
		targetMock3.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsPartialFailureFirst() {
		reset(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(false, true, true);
		targetMock2.handleMessage(messageMock);
		expectLastCall();
		targetMock3.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsPartialFailureMiddle() {
		reset(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(true, false, true);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		targetMock3.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsPartialFailureLast() {
		reset(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(true, true, false);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		targetMock2.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsAllFail() {
		reset(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(false, false, false);
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void noDuplicateSubscription() {
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void removeConsumerBeforeSend() {
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		dispatcher.removeHandler(targetMock2);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		targetMock3.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void removeConsumerBetweenSends() {
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		targetMock1.handleMessage(messageMock);
		expectLastCall().times(2);
		targetMock2.handleMessage(messageMock);
		expectLastCall();
		targetMock3.handleMessage(messageMock);
		expectLastCall().times(2);
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		dispatcher.removeHandler(targetMock2);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void applySequenceDisabledByDefault() {
		BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();
		final List<Message<?>> messages = Collections.synchronizedList(new ArrayList<Message<?>>());
		MessageHandler target1 = new MessageStoringTestEndpoint(messages);
		MessageHandler target2 = new MessageStoringTestEndpoint(messages);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.dispatch(new StringMessage("test"));
		assertEquals(2, messages.size());
		assertEquals(0, (int) messages.get(0).getHeaders().getSequenceNumber());
		assertEquals(0, (int) messages.get(0).getHeaders().getSequenceSize());
		assertEquals(0, (int) messages.get(1).getHeaders().getSequenceNumber());
		assertEquals(0, (int) messages.get(1).getHeaders().getSequenceSize());
	}

	@Test
	public void applySequenceEnabled() {
		BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();
		dispatcher.setApplySequence(true);
		final List<Message<?>> messages = Collections.synchronizedList(new ArrayList<Message<?>>());
		MessageHandler target1 = new MessageStoringTestEndpoint(messages);
		MessageHandler target2 = new MessageStoringTestEndpoint(messages);
		MessageHandler target3 = new MessageStoringTestEndpoint(messages);
		dispatcher.addHandler(target1);
		dispatcher.addHandler(target2);
		dispatcher.addHandler(target3);
		Message<?> inputMessage = new StringMessage("test");
		Object originalId = inputMessage.getHeaders().getId();
		dispatcher.dispatch(inputMessage);
		assertEquals(3, messages.size());
		assertEquals(1, (int) messages.get(0).getHeaders().getSequenceNumber());
		assertEquals(3, (int) messages.get(0).getHeaders().getSequenceSize());
		assertEquals(originalId, messages.get(0).getHeaders().getCorrelationId());
		assertEquals(2, (int) messages.get(1).getHeaders().getSequenceNumber());
		assertEquals(3, (int) messages.get(1).getHeaders().getSequenceSize());
		assertEquals(originalId, messages.get(1).getHeaders().getCorrelationId());
		assertEquals(3, (int) messages.get(2).getHeaders().getSequenceNumber());
		assertEquals(3, (int) messages.get(2).getHeaders().getSequenceSize());
		assertEquals(originalId, messages.get(2).getHeaders().getCorrelationId());
	}


	private void defaultTaskExecutorMock() {
		taskExecutorMock.execute(isA(Runnable.class));
		expectLastCall().andAnswer(new IAnswer<Object>() {
			public Object answer() throws Throwable {
				((Runnable) getCurrentArguments()[0]).run();
				return null;
			}
		}).anyTimes();
	}

	/*
	 * runs the runnable based on the array of passes
	 */
	private void partialFailingExecutorMock(boolean... passes) {
		taskExecutorMock.execute(isA(Runnable.class));
		for (final boolean pass : passes) {
			expectLastCall().andAnswer(new IAnswer<Object>() {
				public Object answer() throws Throwable {
					if (pass) {
						((Runnable) getCurrentArguments()[0]).run();
					}
					return null;
				}
			});
		}
	}


	private static class MessageStoringTestEndpoint implements MessageHandler {

		private final List<Message<?>> messageList;

		MessageStoringTestEndpoint(List<Message<?>> messageList) {
			this.messageList = messageList;
		}

		public void handleMessage(Message<?> message) {
			this.messageList.add(message);
		}
	};

}
