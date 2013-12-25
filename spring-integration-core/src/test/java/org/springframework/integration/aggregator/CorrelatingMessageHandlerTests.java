/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Iwein Fuld
 * @author Dave Syer
 */
@RunWith(MockitoJUnitRunner.class)
public class CorrelatingMessageHandlerTests {

	private AggregatingMessageHandler handler;

	@Mock
	private CorrelationStrategy correlationStrategy;

	private final ReleaseStrategy ReleaseStrategy = new SequenceSizeReleaseStrategy();

	@Mock
	private MessageGroupProcessor processor;

	@Mock
	private MessageChannel outputChannel;

	private final MessageGroupStore store = new SimpleMessageStore();


	@Before
	public void initializeSubject() {
		handler = new AggregatingMessageHandler(processor, store, correlationStrategy, ReleaseStrategy);
		handler.setOutputChannel(outputChannel);
	}


	@Test
	public void bufferCompletesNormally() throws Exception {
		String correlationKey = "key";
		Message<?> message1 = testMessage(correlationKey, 1, 2);
		Message<?> message2 = testMessage(correlationKey, 2, 2);

		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(correlationKey);
		when(processor.processMessageGroup(any(MessageGroup.class))).thenReturn(MessageBuilder.withPayload("grouped").build());
		when(outputChannel.send(any(Message.class), anyLong())).thenReturn(true);

		handler.handleMessage(message1);

		handler.handleMessage(message2);

		verify(correlationStrategy).getCorrelationKey(message1);
		verify(correlationStrategy).getCorrelationKey(message2);
		verify(processor).processMessageGroup(isA(SimpleMessageGroup.class));
	}

	@Test
	public void bufferCompletesWithException() throws Exception {

		doAnswer(new ThrowsException(new RuntimeException("Planned test exception")))
				.when(processor).processMessageGroup(isA(SimpleMessageGroup.class));

		String correlationKey = "key";
		Message<?> message1 = testMessage(correlationKey, 1, 2);
		Message<?> message2 = testMessage(correlationKey, 2, 2);

		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(correlationKey);

		handler.setExpireGroupsUponCompletion(true);

		handler.handleMessage(message1);

		try {
			handler.handleMessage(message2);
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			assertEquals(0, store.getMessageGroup(correlationKey).size());
		}

		verify(correlationStrategy).getCorrelationKey(message1);
		verify(correlationStrategy).getCorrelationKey(message2);
		verify(processor).processMessageGroup(isA(SimpleMessageGroup.class));
	}

	/*
	 * The next test verifies that when pruning happens after the completing message arrived, but before the group was
	 * processed locking prevents forced completion and the group completes normally.
	 */

	@Test
	public void shouldNotPruneWhileCompleting() throws Exception {
		String correlationKey = "key";
		final Message<?> message1 = testMessage(correlationKey, 1, 2);
		final Message<?> message2 = testMessage(correlationKey, 2, 2);
		final List<Message<?>> storedMessages = new ArrayList<Message<?>>();

		final CountDownLatch bothMessagesHandled = new CountDownLatch(2);

		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(correlationKey);
		when(processor.processMessageGroup(any(MessageGroup.class))).thenReturn(MessageBuilder.withPayload("grouped").build());
		when(outputChannel.send(any(Message.class), anyLong())).thenReturn(true);

		handler.handleMessage(message1);
		bothMessagesHandled.countDown();
		storedMessages.add(message1);
		Executors.newSingleThreadExecutor().submit(new Runnable() {
			public void run() {
				handler.handleMessage(message2);
				storedMessages.add(message2);
				bothMessagesHandled.countDown();
			}
		});

		assertTrue(bothMessagesHandled.await(10, TimeUnit.SECONDS));

		assertEquals(0, store.expireMessageGroups(10000));
	}

	@Test
	public void testNullCorrelationKey() throws Exception {
		final Message<?> message1 = MessageBuilder.withPayload("foo").build();
		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(null);
		try {
			handler.handleMessage(message1);
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			Throwable cause = e.getCause();
			boolean pass = cause instanceof IllegalStateException && cause.getMessage().toLowerCase().contains("null correlation");
			if (!pass) {
				throw e;
			}
		}
	}


	private Message<?> testMessage(String correlationKey, int sequenceNumber, int sequenceSize) {
		return MessageBuilder.withPayload("test" + sequenceNumber).setCorrelationId(correlationKey).setSequenceNumber(
				sequenceNumber).setSequenceSize(sequenceSize).build();
	}

}
