/*
 * Copyright 2002-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroupFactory;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class AggregatorTests  implements TestApplicationContextAware {

	private static final Log logger = LogFactory.getLog(AggregatorTests.class);

	private AggregatingMessageHandler aggregator;

	private final SimpleMessageStore store = new SimpleMessageStore(50);

	private final List<MessageGroupExpiredEvent> expiryEvents = new ArrayList<>();

	@BeforeEach
	public void configureAggregator() {
		this.aggregator = new AggregatingMessageHandler(new MultiplyingProcessor(), store);
		this.aggregator.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		this.aggregator.setApplicationEventPublisher(event -> expiryEvents.add((MessageGroupExpiredEvent) event));
		this.aggregator.setBeanName("testAggregator");
		this.aggregator.afterPropertiesSet();
		expiryEvents.clear();
	}

	@Test
	@Disabled
	public void testAggPerf() throws InterruptedException, ExecutionException, TimeoutException {
		AggregatingMessageHandler handler = new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
		handler.setCorrelationStrategy(message -> "foo");
		handler.setReleaseStrategy(new MessageCountReleaseStrategy(60000));
		handler.setExpireGroupsUponCompletion(true);
		handler.setSendPartialResultOnExpiry(true);
		DirectChannel outputChannel = new DirectChannel();
		handler.setOutputChannel(outputChannel);

		final CompletableFuture<Collection<?>> resultFuture = new CompletableFuture<>();
		outputChannel.subscribe(message -> {
			Collection<?> payload = (Collection<?>) message.getPayload();
			logger.warn("Received " + payload.size());
			resultFuture.complete(payload);
		});

		SimpleMessageStore store = new SimpleMessageStore();

		SimpleMessageGroupFactory messageGroupFactory =
				new SimpleMessageGroupFactory(SimpleMessageGroupFactory.GroupType.LIST);

		store.setMessageGroupFactory(messageGroupFactory);

		handler.setMessageStore(store);

		Message<?> message = new GenericMessage<String>("foo");
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		for (int i = 0; i < 120000; i++) {
			if (i % 10000 == 0) {
				stopwatch.stop();
				logger.warn("Sent " + i + " in " + stopwatch.getTotalTimeSeconds() +
						" (10k in " + stopwatch.lastTaskInfo().getTimeMillis() + "ms)");
				stopwatch.start();
			}
			handler.handleMessage(message);
		}
		stopwatch.stop();
		logger.warn("Sent " + 120000 + " in " + stopwatch.getTotalTimeSeconds() +
				" (10k in " + stopwatch.lastTaskInfo().getTimeMillis() + "ms)");

		Collection<?> result = resultFuture.get(10, TimeUnit.SECONDS);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(60000);
	}

	@Test
	@Disabled("Time sensitive")
	public void testAggPerfDefaultPartial() throws InterruptedException, ExecutionException, TimeoutException {
		AggregatingMessageHandler handler = new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
		handler.setCorrelationStrategy(message -> "foo");
		DirectChannel outputChannel = new DirectChannel();
		handler.setOutputChannel(outputChannel);

		final CompletableFuture<Collection<?>> resultFuture = new CompletableFuture<>();
		outputChannel.subscribe(message -> {
			Collection<?> payload = (Collection<?>) message.getPayload();
			logger.warn("Received " + payload.size());
			resultFuture.complete(payload);
		});

		SimpleMessageStore store = new SimpleMessageStore();

		SimpleMessageGroupFactory messageGroupFactory =
				new SimpleMessageGroupFactory(SimpleMessageGroupFactory.GroupType.BLOCKING_QUEUE);

		store.setMessageGroupFactory(messageGroupFactory);

		handler.setMessageStore(store);

		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		for (int i = 0; i < 120000; i++) {
			if (i % 10000 == 0) {
				stopwatch.stop();
				logger.warn("Sent " + i + " in " + stopwatch.getTotalTimeSeconds() +
						" (10k in " + stopwatch.lastTaskInfo().getTimeMillis() + "ms)");
				stopwatch.start();
			}
			handler.handleMessage(MessageBuilder.withPayload("foo")
					.setSequenceSize(120000)
					.setSequenceNumber(i + 1)
					.build());
		}
		stopwatch.stop();
		logger.warn("Sent " + 120000 + " in " + stopwatch.getTotalTimeSeconds() +
				" (10k in " + stopwatch.lastTaskInfo().getTimeMillis() + "ms)");

		Collection<?> result = resultFuture.get(10, TimeUnit.SECONDS);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(120000);
		assertThat(stopwatch.getTotalTimeSeconds()).isLessThan(60.0); // actually < 2.0, was many minutes
	}

	@Test
	public void testCustomAggPerf() throws InterruptedException, ExecutionException, TimeoutException {
		class CustomHandler extends AbstractMessageHandler {

			// custom aggregator, only handles a single correlation

			private final ReentrantLock lock = new ReentrantLock();

			private final Collection<Message<?>> messages = new ArrayList<>(60000);

			private final MessageChannel outputChannel;

			private CustomHandler(MessageChannel outputChannel) {
				this.outputChannel = outputChannel;
			}

			@Override
			public void handleMessageInternal(Message<?> requestMessage) {
				lock.lock();
				try {
					this.messages.add(requestMessage);
					if (this.messages.size() == 60000) {
						List<Object> payloads = new ArrayList<Object>(this.messages.size());
						for (Message<?> message : this.messages) {
							payloads.add(message.getPayload());
						}
						this.messages.clear();
						outputChannel.send(getMessageBuilderFactory().withPayload(payloads)
								.copyHeaders(requestMessage.getHeaders())
								.build());
					}
				}
				finally {
					lock.unlock();
				}
			}

		}

		DirectChannel outputChannel = new DirectChannel();
		CustomHandler handler = new CustomHandler(outputChannel);

		final CompletableFuture<Collection<?>> resultFuture = new CompletableFuture<>();
		outputChannel.subscribe(message -> {
			Collection<?> payload = (Collection<?>) message.getPayload();
			logger.warn("Received " + payload.size());
			resultFuture.complete(payload);
		});
		Message<?> message = new GenericMessage<String>("foo");
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		for (int i = 0; i < 120000; i++) {
			if (i % 10000 == 0) {
				stopwatch.stop();
				logger.warn("Sent " + i + " in " + stopwatch.getTotalTimeSeconds() +
						" (10k in " + stopwatch.lastTaskInfo().getTimeMillis() + "ms)");
				stopwatch.start();
			}
			handler.handleMessage(message);
		}
		stopwatch.stop();
		logger.warn("Sent " + 120000 + " in " + stopwatch.getTotalTimeSeconds() +
				" (10k in " + stopwatch.lastTaskInfo().getTimeMillis() + "ms)");

		Collection<?> result = resultFuture.get(10, TimeUnit.SECONDS);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(60000);
	}

	@Test
	public void testCompleteGroupWithinTimeout() {
		QueueChannel replyChannel = new QueueChannel();
		MessageChannel lockCheckingChannel = (m, to) -> {
			checkLock(this.aggregator, "ABC", true);
			replyChannel.send(m);
			return true;
		};
		Message<?> message1 = createMessage(3, "ABC", 3, 1, lockCheckingChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, lockCheckingChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, lockCheckingChannel, null);

		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);

		Message<?> reply = replyChannel.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(105).isEqualTo(reply.getPayload());
	}

	@Test
	public void testCompleteGroupWithinTimeoutUnlockB4Send() {
		QueueChannel replyChannel = new QueueChannel();
		MessageChannel lockCheckingChannel = (m, to) -> {
			checkLock(this.aggregator, "ABC", false);
			replyChannel.send(m);
			return true;
		};
		Message<?> message1 = createMessage(3, "ABC", 3, 1, lockCheckingChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, lockCheckingChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, lockCheckingChannel, null);

		this.aggregator.setReleaseLockBeforeSend(true);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);

		Message<?> reply = replyChannel.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(105).isEqualTo(reply.getPayload());
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefault() {
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel((m, to) -> {
			checkLock(this.aggregator, "ABC", true);
			discardChannel.send(m);
			return true;
		});
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage(3, "ABC", 2, 1, replyChannel, null);
		this.aggregator.handleMessage(message);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).as("No message should have been sent normally").isNull();
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertThat(discardedMessage).as("A message should have been discarded").isNotNull();
		assertThat(discardedMessage).isEqualTo(message);
		assertThat(expiryEvents.size()).isEqualTo(1);
		assertThat(expiryEvents.get(0).getSource()).isSameAs(this.aggregator);
		assertThat(this.expiryEvents.get(0).getGroupId()).isEqualTo("ABC");
		assertThat(this.expiryEvents.get(0).getMessageCount()).isEqualTo(1);
		assertThat(this.expiryEvents.get(0).isDiscarded()).isTrue();
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefaultUnlockB4Send() {
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel((m, to) -> {
			checkLock(this.aggregator, "ABC", false);
			discardChannel.send(m);
			return true;
		});
		this.aggregator.setReleaseLockBeforeSend(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage(3, "ABC", 2, 1, replyChannel, null);
		this.aggregator.handleMessage(message);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).as("No message should have been sent normally").isNull();
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertThat(discardedMessage).as("A message should have been discarded").isNotNull();
		assertThat(discardedMessage).isEqualTo(message);
		assertThat(expiryEvents.size()).isEqualTo(1);
		assertThat(expiryEvents.get(0).getSource()).isSameAs(this.aggregator);
		assertThat(this.expiryEvents.get(0).getGroupId()).isEqualTo("ABC");
		assertThat(this.expiryEvents.get(0).getMessageCount()).isEqualTo(1);
		assertThat(this.expiryEvents.get(0).isDiscarded()).isTrue();
	}

	@Test
	public void testShouldSendPartialResultOnTimeoutTrue() {
		this.aggregator.setSendPartialResultOnExpiry(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertThat(reply).as("A reply message should have been received").isNotNull();
		assertThat(reply.getPayload()).isEqualTo(15);
		assertThat(expiryEvents.size()).isEqualTo(1);
		assertThat(expiryEvents.get(0).getSource()).isSameAs(this.aggregator);
		assertThat(this.expiryEvents.get(0).getGroupId()).isEqualTo("ABC");
		assertThat(this.expiryEvents.get(0).getMessageCount()).isEqualTo(2);
		assertThat(this.expiryEvents.get(0).isDiscarded()).isFalse();
		Message<?> message3 = createMessage(5, "ABC", 3, 3, replyChannel, null);
		this.aggregator.handleMessage(message3);
		assertThat(this.store.getMessageGroup("ABC").size()).isEqualTo(1);
	}

	@Test
	public void testGroupRemainsAfterTimeout() {
		this.aggregator.setSendPartialResultOnExpiry(true);
		this.aggregator.setExpireGroupsUponTimeout(false);
		QueueChannel replyChannel = new QueueChannel();
		MessageChannel lockCheckingChannel = (m, to) -> {
			checkLock(this.aggregator, "ABC", true);
			replyChannel.send(m);
			return true;
		};
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel((m, to) -> {
			checkLock(this.aggregator, "ABC", true);
			discardChannel.send(m);
			return true;
		});
		Message<?> message1 = createMessage(3, "ABC", 3, 1, lockCheckingChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, lockCheckingChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertThat(reply).as("A reply message should have been received").isNotNull();
		assertThat(reply.getPayload()).isEqualTo(15);
		assertThat(expiryEvents.size()).isEqualTo(1);
		assertThat(expiryEvents.get(0).getSource()).isSameAs(this.aggregator);
		assertThat(this.expiryEvents.get(0).getGroupId()).isEqualTo("ABC");
		assertThat(this.expiryEvents.get(0).getMessageCount()).isEqualTo(2);
		assertThat(this.expiryEvents.get(0).isDiscarded()).isFalse();
		assertThat(this.store.getMessageGroup("ABC").size()).isEqualTo(0);
		Message<?> message3 = createMessage(5, "ABC", 3, 3, lockCheckingChannel, null);
		this.aggregator.handleMessage(message3);
		assertThat(this.store.getMessageGroup("ABC").size()).isEqualTo(0);
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertThat(discardedMessage).as("A message should have been discarded").isNotNull();
		assertThat(discardedMessage).isSameAs(message3);
	}

	@Test
	public void testGroupRemainsAfterTimeoutUnlockB4Discard() {
		this.aggregator.setSendPartialResultOnExpiry(true);
		this.aggregator.setExpireGroupsUponTimeout(false);
		this.aggregator.setReleaseLockBeforeSend(true);
		QueueChannel replyChannel = new QueueChannel();
		MessageChannel lockCheckingChannel = (m, to) -> {
			checkLock(this.aggregator, "ABC", false);
			replyChannel.send(m);
			return true;
		};
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel((m, to) -> {
			checkLock(this.aggregator, "ABC", false);
			discardChannel.send(m);
			return true;
		});
		Message<?> message1 = createMessage(3, "ABC", 3, 1, lockCheckingChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, lockCheckingChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertThat(reply).as("A reply message should have been received").isNotNull();
		assertThat(reply.getPayload()).isEqualTo(15);
		assertThat(expiryEvents.size()).isEqualTo(1);
		assertThat(expiryEvents.get(0).getSource()).isSameAs(this.aggregator);
		assertThat(this.expiryEvents.get(0).getGroupId()).isEqualTo("ABC");
		assertThat(this.expiryEvents.get(0).getMessageCount()).isEqualTo(2);
		assertThat(this.expiryEvents.get(0).isDiscarded()).isFalse();
		assertThat(this.store.getMessageGroup("ABC").size()).isEqualTo(0);
		Message<?> message3 = createMessage(5, "ABC", 3, 3, lockCheckingChannel, null);
		this.aggregator.handleMessage(message3);
		assertThat(this.store.getMessageGroup("ABC").size()).isEqualTo(0);
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertThat(discardedMessage).as("A message should have been discarded").isNotNull();
		assertThat(discardedMessage).isSameAs(message3);
	}

	@Test
	public void testMultipleGroupsSimultaneously() {
		QueueChannel replyChannel1 = new QueueChannel();
		QueueChannel replyChannel2 = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel1, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel1, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel1, null);
		Message<?> message4 = createMessage(11, "XYZ", 3, 1, replyChannel2, null);
		Message<?> message5 = createMessage(13, "XYZ", 3, 2, replyChannel2, null);
		Message<?> message6 = createMessage(17, "XYZ", 3, 3, replyChannel2, null);
		aggregator.handleMessage(message1);
		aggregator.handleMessage(message5);
		aggregator.handleMessage(message3);
		aggregator.handleMessage(message6);
		aggregator.handleMessage(message4);
		aggregator.handleMessage(message2);
		@SuppressWarnings("unchecked")
		Message<Integer> reply1 = (Message<Integer>) replyChannel1.receive(1000);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo(105);
		@SuppressWarnings("unchecked")
		Message<Integer> reply2 = (Message<Integer>) replyChannel2.receive(1000);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo(2431);
	}

	@Test
	@Disabled
	// dropped backwards compatibility for setting capacity limit (it's always Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityAtLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();

		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(1);
		this.aggregator.handleMessage(createMessage(3, 2, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(3);
		this.aggregator.handleMessage(createMessage(4, 3, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(4);
		// next message with same correllation ID is discarded
		this.aggregator.handleMessage(createMessage(2, 1, 1, 1, replyChannel, null));
		assertThat(discardChannel.receive(1000).getPayload()).isEqualTo(2);
	}

	@Test
	@Disabled
	// dropped backwards compatibility for setting capacity limit (it's always Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityPassesLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();

		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(1);
		this.aggregator.handleMessage(createMessage(2, 2, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(2);
		this.aggregator.handleMessage(createMessage(3, 3, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(3);
		this.aggregator.handleMessage(createMessage(4, 4, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(4);
		this.aggregator.handleMessage(createMessage(5, 1, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(5);
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void testExceptionThrownIfNoCorrelationId() throws InterruptedException {
		Message<?> message = createMessage(3, null, 2, 1, new QueueChannel(), null);
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.aggregator.handleMessage(message));
	}

	@Test
	public void testAdditionalMessageAfterCompletion() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		Message<?> message4 = createMessage(7, "ABC", 3, 3, replyChannel, null);

		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);
		this.aggregator.handleMessage(message4);

		Message<?> reply = replyChannel.receive(10000);
		assertThat(reply).as("A message should be aggregated").isNotNull();
		assertThat((reply.getPayload())).isEqualTo(105);
	}

	@Test
	public void shouldRejectDuplicatedSequenceNumbers() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		Message<?> message4 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		this.aggregator.setReleaseStrategy(new SequenceSizeReleaseStrategy());

		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message3);
		// duplicated sequence number, either message3 or message4 should be rejected
		this.aggregator.handleMessage(message4);
		this.aggregator.handleMessage(message2);

		Message<?> reply = replyChannel.receive(10000);
		assertThat(reply).as("A message should be aggregated").isNotNull();
		assertThat((reply.getPayload())).isEqualTo(105);
	}

	private static Message<?> createMessage(Object payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel, String predefinedId) {
		MessageBuilder<Object> builder = MessageBuilder.withPayload(payload).setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize).setSequenceNumber(sequenceNumber).setReplyChannel(replyChannel);
		if (predefinedId != null) {
			builder.setHeader(MessageHeaders.ID, predefinedId);
		}
		return builder.build();
	}

	private void checkLock(AbstractCorrelatingMessageHandler handler, String group, boolean expectedHeld) {
		ReentrantLock lock = (ReentrantLock) TestUtils.<LockRegistry<?>>getPropertyValue(handler, "lockRegistry")
				.obtain(UUIDConverter.getUUID(group).toString());
		assertThat(lock.isHeldByCurrentThread()).isEqualTo(expectedHeld);
	}

	private class MultiplyingProcessor implements MessageGroupProcessor {

		MultiplyingProcessor() {
			super();
		}

		@Override
		public Object processMessageGroup(MessageGroup group) {
			int product = 1;
			for (Message<?> message : group.getMessages()) {
				product *= (Integer) message.getPayload();
			}
			return product;
		}

	}

}
