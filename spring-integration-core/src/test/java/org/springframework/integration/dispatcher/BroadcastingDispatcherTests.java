/*
 * Copyright 2002-2017 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 * @author Meherzad Lahewala
 */
public class BroadcastingDispatcherTests {

	private BroadcastingDispatcher dispatcher;

	private final TaskExecutor taskExecutorMock = Mockito.mock(TaskExecutor.class);

	private final Message<?> messageMock = Mockito.mock(Message.class);

	private final MessageHandler targetMock1 = Mockito.mock(MessageHandler.class);

	private final MessageHandler targetMock2 = Mockito.mock(MessageHandler.class);

	private final MessageHandler targetMock3 = Mockito.mock(MessageHandler.class);


	@Before
	public void init() {
		Mockito.reset(taskExecutorMock, messageMock, taskExecutorMock, targetMock1, targetMock2, targetMock3);
		defaultTaskExecutorMock();
	}


	@Test
	public void singleTargetWithoutTaskExecutor() throws Exception {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.addHandler(targetMock1);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
	}

	@Test
	public void singleTargetWithTaskExecutor() throws Exception {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
	}

	@Test
	public void multipleTargetsWithoutTaskExecutor() {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
		verify(targetMock2).handleMessage(eq(messageMock));
		verify(targetMock3).handleMessage(eq(messageMock));
	}

	@Test
	public void multipleTargetsWithTaskExecutor() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
		verify(targetMock2).handleMessage(eq(messageMock));
		verify(targetMock3).handleMessage(eq(messageMock));
	}

	@Test
	public void multipleTargetsPartialFailureFirst() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(false, true, true);
		dispatcher.dispatch(messageMock);
		verify(targetMock1, Mockito.never()).handleMessage(eq(messageMock));
		verify(targetMock2).handleMessage(eq(messageMock));
		verify(targetMock3).handleMessage(eq(messageMock));
	}

	@Test
	public void multipleTargetsPartialFailureMiddle() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(true, false, true);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
		verify(targetMock2, Mockito.never()).handleMessage(eq(messageMock));
		verify(targetMock3).handleMessage(eq(messageMock));
	}

	@Test
	public void multipleTargetsPartialFailureLast() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(true, true, false);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
		verify(targetMock2).handleMessage(eq(messageMock));
		verify(targetMock3, Mockito.never()).handleMessage(eq(messageMock));
	}

	@Test
	public void multipleTargetsAllFail() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		partialFailingExecutorMock(false, false, false);
		dispatcher.dispatch(messageMock);
		verify(targetMock1, Mockito.never()).handleMessage(eq(messageMock));
		verify(targetMock2, Mockito.never()).handleMessage(eq(messageMock));
		verify(targetMock3, Mockito.never()).handleMessage(eq(messageMock));
	}

	@Test
	public void noDuplicateSubscription() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock1);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
	}

	@Test
	public void removeConsumerBeforeSend() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		dispatcher.removeHandler(targetMock2);
		dispatcher.dispatch(messageMock);
		verify(targetMock1).handleMessage(eq(messageMock));
		verify(targetMock2, Mockito.never()).handleMessage(eq(messageMock));
		verify(targetMock3).handleMessage(eq(messageMock));
	}

	@Test
	public void removeConsumerBetweenSends() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		dispatcher.addHandler(targetMock1);
		dispatcher.addHandler(targetMock2);
		dispatcher.addHandler(targetMock3);
		dispatcher.dispatch(messageMock);
		dispatcher.removeHandler(targetMock2);
		dispatcher.dispatch(messageMock);
		verify(targetMock1, Mockito.times(2)).handleMessage(eq(messageMock));
		verify(targetMock2).handleMessage(eq(messageMock));
		verify(targetMock3, Mockito.times(2)).handleMessage(eq(messageMock));
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
		assertEquals(0, new IntegrationMessageHeaderAccessor(messages.get(0)).getSequenceNumber());
		assertEquals(0, new IntegrationMessageHeaderAccessor(messages.get(0)).getSequenceSize());
		assertEquals(0, new IntegrationMessageHeaderAccessor(messages.get(1)).getSequenceNumber());
		assertEquals(0, new IntegrationMessageHeaderAccessor(messages.get(1)).getSequenceSize());
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
		assertEquals(1, new IntegrationMessageHeaderAccessor(messages.get(0)).getSequenceNumber());
		assertEquals(3, new IntegrationMessageHeaderAccessor(messages.get(0)).getSequenceSize());
		assertEquals(originalId, new IntegrationMessageHeaderAccessor(messages.get(0)).getCorrelationId());
		assertEquals(2, new IntegrationMessageHeaderAccessor(messages.get(1)).getSequenceNumber());
		assertEquals(3, new IntegrationMessageHeaderAccessor(messages.get(1)).getSequenceSize());
		assertEquals(originalId, new IntegrationMessageHeaderAccessor(messages.get(1)).getCorrelationId());
		assertEquals(3, new IntegrationMessageHeaderAccessor(messages.get(2)).getSequenceNumber());
		assertEquals(3, new IntegrationMessageHeaderAccessor(messages.get(2)).getSequenceSize());
		assertEquals(originalId, new IntegrationMessageHeaderAccessor(messages.get(2)).getCorrelationId());
	}

	/**
	 * Verifies that the dispatcher adds the message to the exception if it
	 * was not attached by the handler.
	 */
	@Test
	public void testExceptionEnhancement() {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.addHandler(targetMock1);
		doThrow(new MessagingException("Mock Exception"))
				.when(targetMock1).handleMessage(eq(messageMock));
		try {
			dispatcher.dispatch(messageMock);
			fail("Expected Exception");
		}
		catch (MessagingException e) {
			assertEquals(messageMock, e.getFailedMessage());
		}
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
		doThrow(new MessagingException(dontReplaceThisMessage, "Mock Exception"))
				.when(targetMock1).handleMessage(eq(messageMock));
		try {
			dispatcher.dispatch(messageMock);
			fail("Expected Exception");
		}
		catch (MessagingException e) {
			assertEquals(dontReplaceThisMessage, e.getFailedMessage());
		}
	}

	/**
	 * Verifies behavior when no handler is subscribed to the dispatcher.
	 */
	@Test
	public void testNoHandler() {
		dispatcher = new BroadcastingDispatcher();
		assertTrue(dispatcher.dispatch(messageMock));
	}

	/**
	 * Verifies behavior when no handler is subscribed to the dispatcher with executor.
	 */
	@Test
	public void testNoHandlerWithExecutor() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock);
		assertTrue(dispatcher.dispatch(messageMock));
	}

	/**
	 * Verifies behavior when dispatcher throws exception if no handler is subscribed
	 * and requireSubscribers set to true.
	 */
	@Test
	public void testNoHandlerWithRequiredSubscriber() {
		dispatcher = new BroadcastingDispatcher(true);
		try {
			dispatcher.dispatch(messageMock);
			fail("Expected Exception");
		}
		catch (MessageDispatchingException exception) {
			assertEquals(messageMock, exception.getFailedMessage());
		}
	}

	/**
	 * Verifies behavior when dispatcher with executors throws exception if no handler is subscribed
	 * and requireSubscribers set to true.
	 */
	@Test
	public void testNoHandlerWithExecutorWithRequiredSubscriber() {
		dispatcher = new BroadcastingDispatcher(taskExecutorMock, true);
		try {
			dispatcher.dispatch(messageMock);
			fail("Expected Exception");
		}
		catch (MessageDispatchingException exception) {
			assertEquals(messageMock, exception.getFailedMessage());
		}
	}

	private void defaultTaskExecutorMock() {
		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(taskExecutorMock).execute(Mockito.any(Runnable.class));
	}

	/*
	 * runs the runnable based on the array of passes
	 */
	private void partialFailingExecutorMock(final boolean... passes) {
		final AtomicInteger count = new AtomicInteger();
		doAnswer(invocation -> {
			if (passes[count.getAndIncrement()]) {
				((Runnable) invocation.getArgument(0)).run();
			}
			return null;
		}).when(taskExecutorMock).execute(Mockito.any(Runnable.class));
	}


	private static class MessageStoringTestEndpoint implements MessageHandler {

		private final List<Message<?>> messageList;

		MessageStoringTestEndpoint(List<Message<?>> messageList) {
			this.messageList = messageList;
		}

		@Override
		public void handleMessage(Message<?> message) {
			this.messageList.add(message);
		}

	}

}
