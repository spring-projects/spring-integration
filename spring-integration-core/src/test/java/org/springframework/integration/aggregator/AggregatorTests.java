/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 * @author Gary Russell
 */
public class AggregatorTests {

	private static final Log logger = LogFactory.getLog(AggregatorTests.class);

	private AggregatingMessageHandler aggregator;

	private final SimpleMessageStore store = new SimpleMessageStore(50);

	List<MessageGroupExpiredEvent> expiryEvents = new ArrayList<MessageGroupExpiredEvent>();

	@Before
	public void configureAggregator() {
		this.aggregator = new AggregatingMessageHandler(new MultiplyingProcessor(), store);
		this.aggregator.setBeanFactory(mock(BeanFactory.class));
		this.aggregator.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				expiryEvents.add((MessageGroupExpiredEvent) event);
			}
		});
		this.aggregator.setBeanName("testAggregator");
		this.aggregator.afterPropertiesSet();
		expiryEvents.clear();
	}

	@Test
	public void testAggPerf() {
		AggregatingMessageHandler handler = new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
		handler.setCorrelationStrategy(new CorrelationStrategy() {

			@Override
			public Object getCorrelationKey(Message<?> message) {
				return "foo";
			}
		});
		handler.setReleaseStrategy(new MessageCountReleaseStrategy(60000));
		handler.setExpireGroupsUponCompletion(true);
		handler.setSendPartialResultOnExpiry(true);
		DirectChannel outputChannel = new DirectChannel();
		handler.setOutputChannel(outputChannel);
		outputChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				logger.warn("Received " + ((Collection<?>) message.getPayload()).size());
			}

		});
		Message<?> message = new GenericMessage<String>("foo");
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		for (int i=0; i < 120000; i++) {
			if (i % 10000 == 0) {
				stopwatch.stop();
				logger.warn("Sent " + i + " in " + stopwatch.getTotalTimeSeconds() +
						" (10k in " + stopwatch.getLastTaskTimeMillis() + "ms)");
				stopwatch.start();
			}
			handler.handleMessage(message);
		}
		stopwatch.stop();
		logger.warn("Sent " + 120000 + " in " + stopwatch.getTotalTimeSeconds() +
				" (10k in " + stopwatch.getLastTaskTimeMillis() + "ms)");
	}

	@Test
	public void testCustomAggPerf() {
		class CustomHandler extends AbstractMessageHandler {

			// custom aggregator, only handles a single correlation

			private final ReentrantLock lock = new ReentrantLock();

			private final Collection<Message<?>> messages = new ArrayList<Message<?>>(60000);

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
		outputChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				logger.warn("Received " + ((Collection<?>) message.getPayload()).size());
			}

		});
		Message<?> message = new GenericMessage<String>("foo");
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		for (int i=0; i < 120000; i++) {
			if (i % 10000 == 0) {
				stopwatch.stop();
				logger.warn("Sent " + i + " in " + stopwatch.getTotalTimeSeconds() +
						" (10k in " + stopwatch.getLastTaskTimeMillis() + "ms)");
				stopwatch.start();
			}
			handler.handleMessage(message);
		}
		stopwatch.stop();
		logger.warn("Sent " + 120000 + " in " + stopwatch.getTotalTimeSeconds() +
				" (10k in " + stopwatch.getLastTaskTimeMillis() + "ms)");
	}

	@Test
	public void testCompleteGroupWithinTimeout() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);

		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);

		Message<?> reply = replyChannel.receive(10000);
		assertNotNull(reply);
		assertEquals(reply.getPayload(), 105);
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefault() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage(3, "ABC", 2, 1, replyChannel, null);
		this.aggregator.handleMessage(message);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertNull("No message should have been sent normally", reply);
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertNotNull("A message should have been discarded", discardedMessage);
		assertEquals(message, discardedMessage);
		assertEquals(1, expiryEvents.size());
		assertSame(this.aggregator, expiryEvents.get(0).getSource());
		assertEquals("ABC", this.expiryEvents.get(0).getGroupId());
		assertEquals(1, this.expiryEvents.get(0).getMessageCount());
		assertEquals(true, this.expiryEvents.get(0).isDiscarded());
	}

	@Test
	public void testShouldSendPartialResultOnTimeoutTrue() throws InterruptedException {
		this.aggregator.setSendPartialResultOnExpiry(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertNotNull("A reply message should have been received", reply);
		assertEquals(15, reply.getPayload());
		assertEquals(1, expiryEvents.size());
		assertSame(this.aggregator, expiryEvents.get(0).getSource());
		assertEquals("ABC", this.expiryEvents.get(0).getGroupId());
		assertEquals(2, this.expiryEvents.get(0).getMessageCount());
		assertEquals(false, this.expiryEvents.get(0).isDiscarded());
		Message<?> message3 = createMessage(5, "ABC", 3, 3, replyChannel, null);
		this.aggregator.handleMessage(message3);
		assertEquals(1, this.store.getMessageGroup("ABC").size());
	}

	@Test
	public void testGroupRemainsAfterTimeout() throws InterruptedException {
		this.aggregator.setSendPartialResultOnExpiry(true);
		this.aggregator.setExpireGroupsUponTimeout(false);
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel(discardChannel);
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertNotNull("A reply message should have been received", reply);
		assertEquals(15, reply.getPayload());
		assertEquals(1, expiryEvents.size());
		assertSame(this.aggregator, expiryEvents.get(0).getSource());
		assertEquals("ABC", this.expiryEvents.get(0).getGroupId());
		assertEquals(2, this.expiryEvents.get(0).getMessageCount());
		assertEquals(false, this.expiryEvents.get(0).isDiscarded());
		assertEquals(0, this.store.getMessageGroup("ABC").size());
		Message<?> message3 = createMessage(5, "ABC", 3, 3, replyChannel, null);
		this.aggregator.handleMessage(message3);
		assertEquals(0, this.store.getMessageGroup("ABC").size());
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertNotNull("A message should have been discarded", discardedMessage);
		assertSame(message3, discardedMessage);
	}

	@Test
	public void testMultipleGroupsSimultaneously() throws InterruptedException {
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
		assertNotNull(reply1);
		assertThat(reply1.getPayload(), is(105));
		@SuppressWarnings("unchecked")
		Message<Integer> reply2 = (Message<Integer>) replyChannel2.receive(1000);
		assertNotNull(reply2);
		assertThat(reply2.getPayload(), is(2431));
	}

	@Test
	@Ignore
	// dropped backwards compatibility for setting capacity limit (it's always Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityAtLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();

		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertEquals(1, replyChannel.receive(1000).getPayload());
		this.aggregator.handleMessage(createMessage(3, 2, 1, 1, replyChannel, null));
		assertEquals(3, replyChannel.receive(1000).getPayload());
		this.aggregator.handleMessage(createMessage(4, 3, 1, 1, replyChannel, null));
		assertEquals(4, replyChannel.receive(1000).getPayload());
		// next message with same correllation ID is discarded
		this.aggregator.handleMessage(createMessage(2, 1, 1, 1, replyChannel, null));
		assertEquals(2, discardChannel.receive(1000).getPayload());
	}

	@Test
	@Ignore
	// dropped backwards compatibility for setting capacity limit (it's always Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityPassesLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();

		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertEquals(1, replyChannel.receive(1000).getPayload());
		this.aggregator.handleMessage(createMessage(2, 2, 1, 1, replyChannel, null));
		assertEquals(2, replyChannel.receive(1000).getPayload());
		this.aggregator.handleMessage(createMessage(3, 3, 1, 1, replyChannel, null));
		assertEquals(3, replyChannel.receive(1000).getPayload());
		this.aggregator.handleMessage(createMessage(4, 4, 1, 1, replyChannel, null));
		assertEquals(4, replyChannel.receive(1000).getPayload());
		this.aggregator.handleMessage(createMessage(5, 1, 1, 1, replyChannel, null));
		assertEquals(5, replyChannel.receive(1000).getPayload());
		assertNull(discardChannel.receive(0));
	}

	@Test(expected = MessageHandlingException.class)
	public void testExceptionThrownIfNoCorrelationId() throws InterruptedException {
		Message<?> message = createMessage(3, null, 2, 1, new QueueChannel(), null);
		this.aggregator.handleMessage(message);
	}

	@Test
	public void testAdditionalMessageAfterCompletion() throws InterruptedException {
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
		assertNotNull("A message should be aggregated", reply);
		assertThat(((Integer) reply.getPayload()), is(105));
	}

	@Test
	public void shouldRejectDuplicatedSequenceNumbers() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		Message<?> message4 = createMessage(7, "ABC", 3, 3, replyChannel, null);

		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message3);
		// duplicated sequence number, either message3 or message4 should be rejected
		this.aggregator.handleMessage(message4);
		this.aggregator.handleMessage(message2);

		Message<?> reply = replyChannel.receive(10000);
		assertNotNull("A message should be aggregated", reply);
		assertThat(((Integer) reply.getPayload()), is(105));
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


	private class MultiplyingProcessor implements MessageGroupProcessor {
		@Override
		public Object processMessageGroup(MessageGroup group) {
			Integer product = 1;
			for (Message<?> message : group.getMessages()) {
				product *= (Integer) message.getPayload();
			}
			return product;
		}
	}


}
