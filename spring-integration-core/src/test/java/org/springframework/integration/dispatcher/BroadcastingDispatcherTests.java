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

package org.springframework.integration.dispatcher;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.EiMessageHeaderAccessor;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
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
		reset(globalMocks);
		defaultTaskExecutorMock();
	}


	@Test
	public void singleTargetWithoutTaskExecutor() throws Exception {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void singleTargetWithTaskExecutor() throws Exception {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		expectLastCall();
		replay(globalMocks);
		dispatcher.dispatch(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsWithoutTaskExecutor() {
		dispatcher = new BroadcastingDispatcher();
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
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
		dispatcher.dispatch(new GenericMessage<String>("test"));
		assertEquals(2, messages.size());
		assertEquals(0, (int) new EiMessageHeaderAccessor(messages.get(0)).getSequenceNumber());
		assertEquals(0, (int) new EiMessageHeaderAccessor(messages.get(0)).getSequenceSize());
		assertEquals(0, (int) new EiMessageHeaderAccessor(messages.get(1)).getSequenceNumber());
		assertEquals(0, (int) new EiMessageHeaderAccessor(messages.get(1)).getSequenceSize());
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
		Message<?> inputMessage = new GenericMessage<String>("test");
		Object originalId = inputMessage.getHeaders().getId();
		dispatcher.dispatch(inputMessage);
		assertEquals(3, messages.size());
		assertEquals(1, (int) new EiMessageHeaderAccessor(messages.get(0)).getSequenceNumber());
		assertEquals(3, (int) new EiMessageHeaderAccessor(messages.get(0)).getSequenceSize());
		assertEquals(originalId, new EiMessageHeaderAccessor(messages.get(0)).getCorrelationId());
		assertEquals(2, (int) new EiMessageHeaderAccessor(messages.get(1)).getSequenceNumber());
		assertEquals(3, (int) new EiMessageHeaderAccessor(messages.get(1)).getSequenceSize());
		assertEquals(originalId, new EiMessageHeaderAccessor(messages.get(1)).getCorrelationId());
		assertEquals(3, (int) new EiMessageHeaderAccessor(messages.get(2)).getSequenceNumber());
		assertEquals(3, (int) new EiMessageHeaderAccessor(messages.get(2)).getSequenceSize());
		assertEquals(originalId, new EiMessageHeaderAccessor(messages.get(2)).getCorrelationId());
	}

	/**
	 * Verifies that the dispatcher adds the message to the exception if it
	 * was not attached by the handler.
	 */
	@Test
	public void testExceptionEnhancement() {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		expectLastCall().andThrow(new MessagingException("Mock Exception"));
		replay(globalMocks);
		try {
			dispatcher.dispatch(messageMock);
			fail("Expected Exception");
		} catch (MessagingException e) {
			assertEquals(messageMock, e.getFailedMessage());
		}
		verify(globalMocks);
	}

	/**
	 * Verifies that the dispatcher does not add the message to the exception if it
	 * was attached by the handler.
	 */
	@Test
	public void testNoExceptionEnhancement() {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.addHandler(targetMock1);
		targetMock1.handleMessage(messageMock);
		Message<String> dontReplaceThisMessage = MessageBuilder.withPayload("x").build();
		expectLastCall().andThrow(new MessagingException(dontReplaceThisMessage,
				"Mock Exception"));
		replay(globalMocks);
		try {
			dispatcher.dispatch(messageMock);
			fail("Expected Exception");
		} catch (MessagingException e) {
			assertEquals(dontReplaceThisMessage, e.getFailedMessage());
		}
		verify(globalMocks);
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
