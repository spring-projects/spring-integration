/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;

import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Artme Bilan
 */
public class CorrelatingMessageHandlerTests {

	private AggregatingMessageHandler handler;

	private CorrelationStrategy correlationStrategy;

	private final ReleaseStrategy ReleaseStrategy = new SequenceSizeReleaseStrategy();

	private MessageGroupProcessor processor;

	private MessageChannel outputChannel;

	private final MessageGroupStore store = new SimpleMessageStore();

	@BeforeEach
	public void initializeSubject() {
		correlationStrategy = mock(CorrelationStrategy.class);
		processor = mock(MessageGroupProcessor.class);
		outputChannel = mock(MessageChannel.class);
		handler = new AggregatingMessageHandler(processor, store, correlationStrategy, ReleaseStrategy);
		handler.setOutputChannel(outputChannel);
		handler.setBeanFactory(mock());
		handler.afterPropertiesSet();
	}

	@Test
	public void bufferCompletesNormally() {
		String correlationKey = "key";
		Message<?> message1 = testMessage(correlationKey, 1, 2);
		Message<?> message2 = testMessage(correlationKey, 2, 2);

		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(correlationKey);
		when(processor.processMessageGroup(any(MessageGroup.class)))
				.thenReturn(MessageBuilder.withPayload("grouped").build());
		when(outputChannel.send(any(Message.class), eq(30000L))).thenReturn(true);

		handler.handleMessage(message1);

		handler.handleMessage(message2);

		verify(correlationStrategy).getCorrelationKey(message1);
		verify(correlationStrategy).getCorrelationKey(message2);
		verify(processor).processMessageGroup(isA(SimpleMessageGroup.class));
	}

	@Test
	public void bufferCompletesWithException() {

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
			assertThat(store.getMessageGroup(correlationKey).size()).isEqualTo(0);
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

		final CountDownLatch bothMessagesHandled = new CountDownLatch(2);

		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(correlationKey);
		when(processor.processMessageGroup(any(MessageGroup.class)))
				.thenReturn(MessageBuilder.withPayload("grouped").build());
		when(outputChannel.send(any(Message.class), eq(30000L))).thenReturn(true);

		handler.handleMessage(message1);
		bothMessagesHandled.countDown();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.submit(() -> {
			handler.handleMessage(message2);
			bothMessagesHandled.countDown();
		});

		assertThat(bothMessagesHandled.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(store.expireMessageGroups(10000)).isEqualTo(0);
		exec.shutdownNow();
	}

	@Test
	public void testNullCorrelationKey() {
		final Message<?> message1 = MessageBuilder.withPayload("foo").build();
		when(correlationStrategy.getCorrelationKey(isA(Message.class))).thenReturn(null);
		try {
			handler.handleMessage(message1);
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			Throwable cause = e.getCause();
			boolean pass = cause instanceof IllegalStateException
					&& cause.getMessage().toLowerCase().contains("null correlation");
			if (!pass) {
				throw e;
			}
		}
	}

	private Message<?> testMessage(String correlationKey, int sequenceNumber, int sequenceSize) {
		return MessageBuilder.withPayload("test" + sequenceNumber)
				.setCorrelationId(correlationKey)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.build();
	}

}
