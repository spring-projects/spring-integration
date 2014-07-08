/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.splitter;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Alex Peters
 * @author Artem Bilan
 * @since 4.1
 */
public class StreamingSplitterTests {

	private Message<?> message;

	@Before
	public void setUp() {
		message = new GenericMessage<String>("foo.bar");
	}


	@Test
	public void splitToIterator_sequenceSizeInLastMessageHeader()
			throws Exception {
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(
				messageQuantity));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);

		splitter.handleMessage(message);
		List<Message<?>> receivedMessages = replyChannel.clear();
		Collections.sort(receivedMessages, new Comparator<Message<?>>() {

			public int compare(Message<?> o1, Message<?> o2) {
				return o1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class)
						.compareTo(o2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class));
			}

		});
		assertThat(receivedMessages.get(4)
						.getHeaders()
						.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class),
				is(messageQuantity));
	}

	@Test
	public void splitToIterator_sourceMessageHeadersIncluded() throws Exception {
		String anyHeaderKey = "anyProperty1";
		String anyHeaderValue = "anyValue1";
		message = MessageBuilder.fromMessage(message)
				.setHeader(anyHeaderKey, anyHeaderValue)
				.build();
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(
				messageQuantity));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> receivedMessages = replyChannel.clear();
		assertThat(receivedMessages.size(), is(messageQuantity));
		for (Message<?> reveivedMessage : receivedMessages) {
			MessageHeaders headers = reveivedMessage.getHeaders();
			assertTrue("Unexpected result with: " + headers, headers.containsKey(anyHeaderKey));
			assertThat("Unexpected result with: " + headers,
					headers.get(anyHeaderKey, String.class),
					is(anyHeaderValue));
			assertThat("Unexpected result with: " + headers,
					headers.get(IntegrationMessageHeaderAccessor.CORRELATION_ID, UUID.class),
					is(message.getHeaders().getId()));
		}
	}

	@Test
	public void splitToIterator_allMessagesSent() throws Exception {
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(
				messageQuantity));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);

		splitter.handleMessage(message);
		assertThat(replyChannel.getQueueSize(), is(messageQuantity));
	}

	@Test
	public void splitToIterable_allMessagesSent() throws Exception {
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IterableTestBean(
				messageQuantity));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);

		splitter.handleMessage(message);
		assertThat(replyChannel.getQueueSize(), is(messageQuantity));
	}

	@Test
	public void splitToIterator_allMessagesContainSequenceNumber()
			throws Exception {
		final int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(
				messageQuantity));
		DirectChannel replyChannel = new DirectChannel();
		splitter.setOutputChannel(replyChannel);

		new EventDrivenConsumer(replyChannel, new MessageHandler() {

			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat("Failure with msg: " + message,
						message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class),
						is(Integer.valueOf((String) message.getPayload())));
			}
		}).start();
		splitter.handleMessage(message);
	}

	@Test
	public void splitWithMassiveReplyMessages_allMessagesSent()
			throws Exception {
		final int messageQuantity = 100000;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(
				messageQuantity));
		DirectChannel replyChannel = new DirectChannel();
		splitter.setOutputChannel(replyChannel);

		final AtomicInteger receivedMessageCounter = new AtomicInteger(0);
		new EventDrivenConsumer(replyChannel, new MessageHandler() {

			public void handleMessage(Message<?> message)
					throws MessageRejectedException, MessageHandlingException,
					MessageDeliveryException {
				assertThat("Failure with msg: " + message,
						message.getPayload(),
						is(notNullValue()));
				receivedMessageCounter.incrementAndGet();

			}
		}).start();

		splitter.handleMessage(message);
		assertThat(receivedMessageCounter.get(), is(messageQuantity));
	}

	static class IteratorTestBean {

		final int max;

		AtomicInteger counter = new AtomicInteger(0);

		public IteratorTestBean(int max) {
			this.max = max;
		}

		@Splitter
		public Iterator<String> annotatedMethod(String input) {
			return new Iterator<String>() {

				public boolean hasNext() {
					return counter.get() < max;
				}

				public String next() {
					if (!hasNext()) {
						throw new IllegalStateException("Last element reached!");
					}
					return String.valueOf(counter.incrementAndGet());
				}

				public void remove() {
					throw new AssertionError("not implemented!");

				}

			};
		}
	}

	static class IterableTestBean {

		final int max;

		AtomicInteger counter = new AtomicInteger(0);

		public IterableTestBean(int max) {
			this.max = max;
		}

		@Splitter
		public Iterable<String> annotatedMethod(String input) {
			return new Iterable<String>() {

				public Iterator<String> iterator() {

					return new Iterator<String>() {

						public boolean hasNext() {
							return counter.get() < max;
						}

						public String next() {
							if (!hasNext()) {
								throw new IllegalStateException(
										"Last element reached!");
							}
							return String.valueOf(counter.incrementAndGet());
						}

						public void remove() {
							throw new AssertionError("not implemented!");

						}

					};
				}
			};
		}
	}

}
