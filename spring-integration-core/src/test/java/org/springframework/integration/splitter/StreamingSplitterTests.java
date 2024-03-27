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

package org.springframework.integration.splitter;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Peters
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public class StreamingSplitterTests {

	private Message<?> message;

	@Before
	public void setUp() {
		this.message = new GenericMessage<>("foo.bar");
	}

	@Test
	public void splitToIterator_sequenceSizeInLastMessageHeader() {
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(messageQuantity));
		splitter.setBeanFactory(mock(BeanFactory.class));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.afterPropertiesSet();
		splitter.handleMessage(this.message);
		List<Message<?>> receivedMessages = replyChannel.clear();
		receivedMessages.sort(Comparator.comparing(o ->
				o.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class)));
		assertThat(receivedMessages.get(4)
				.getHeaders()
				.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class)).isEqualTo(messageQuantity);
	}

	@Test
	public void splitToIterator_sourceMessageHeadersIncluded() {
		String anyHeaderKey = "anyProperty1";
		String anyHeaderValue = "anyValue1";
		this.message =
				MessageBuilder.fromMessage(this.message)
						.setHeader(anyHeaderKey, anyHeaderValue)
						.build();
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(messageQuantity));
		splitter.setBeanFactory(mock(BeanFactory.class));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.afterPropertiesSet();
		splitter.handleMessage(this.message);
		List<Message<?>> receivedMessages = replyChannel.clear();
		assertThat(receivedMessages.size()).isEqualTo(messageQuantity);
		for (Message<?> receivedMessage : receivedMessages) {
			MessageHeaders headers = receivedMessage.getHeaders();
			assertThat(headers.containsKey(anyHeaderKey)).as("Unexpected result with: " + headers).isTrue();
			assertThat(headers.get(anyHeaderKey, String.class)).as("Unexpected result with: " + headers)
					.isEqualTo(anyHeaderValue);
			assertThat(headers.get(IntegrationMessageHeaderAccessor.CORRELATION_ID, UUID.class))
					.as("Unexpected result with: " + headers).isEqualTo(message.getHeaders().getId());
		}
	}

	@Test
	public void splitToIterator_allMessagesSent() {
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(messageQuantity));
		splitter.setBeanFactory(mock(BeanFactory.class));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.afterPropertiesSet();
		splitter.handleMessage(this.message);
		assertThat(replyChannel.getQueueSize()).isEqualTo(messageQuantity);
	}

	@Test
	public void splitToIterable_allMessagesSent() {
		int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IterableTestBean(messageQuantity));
		splitter.setBeanFactory(mock(BeanFactory.class));
		QueueChannel replyChannel = new QueueChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.afterPropertiesSet();
		splitter.handleMessage(this.message);
		assertThat(replyChannel.getQueueSize()).isEqualTo(messageQuantity);
	}

	@Test
	public void splitToIterator_allMessagesContainSequenceNumber() {
		final int messageQuantity = 5;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(messageQuantity));
		splitter.setBeanFactory(mock(BeanFactory.class));
		DirectChannel replyChannel = new DirectChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.afterPropertiesSet();

		new EventDrivenConsumer(replyChannel, message -> assertThat(message.getHeaders()
				.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class))
				.as("Failure with msg: " + message).isEqualTo(Integer.valueOf((String) message.getPayload()))).start();
		splitter.handleMessage(this.message);
	}

	@Test
	public void splitWithMassiveReplyMessages_allMessagesSent() {
		final int messageQuantity = 100000;
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(new IteratorTestBean(messageQuantity));
		splitter.setBeanFactory(mock(BeanFactory.class));
		DirectChannel replyChannel = new DirectChannel();
		splitter.setOutputChannel(replyChannel);
		splitter.afterPropertiesSet();
		final AtomicInteger receivedMessageCounter = new AtomicInteger(0);
		new EventDrivenConsumer(replyChannel, message -> {
			assertThat(message.getPayload()).as("Failure with msg: " + message).isNotNull();
			receivedMessageCounter.incrementAndGet();

		}).start();

		splitter.handleMessage(this.message);
		assertThat(receivedMessageCounter.get()).isEqualTo(messageQuantity);
	}

	static class IteratorTestBean {

		final int max;

		AtomicInteger counter = new AtomicInteger(0);

		IteratorTestBean(int max) {
			this.max = max;
		}

		@Splitter
		public Iterator<String> annotatedMethod(String input) {
			return new Iterator<String>() {

				@Override
				public boolean hasNext() {
					return counter.get() < max;
				}

				@Override
				public String next() {
					if (!hasNext()) {
						throw new IllegalStateException("Last element reached!");
					}
					return String.valueOf(counter.incrementAndGet());
				}

				@Override
				public void remove() {
					throw new AssertionError("not implemented!");

				}

			};
		}

	}

	static class IterableTestBean {

		final int max;

		AtomicInteger counter = new AtomicInteger(0);

		IterableTestBean(int max) {
			this.max = max;
		}

		@Splitter
		public Iterable<String> annotatedMethod(String input) {
			return new Iterable<String>() {

				@Override
				public Iterator<String> iterator() {

					return new Iterator<String>() {

						@Override
						public boolean hasNext() {
							return counter.get() < max;
						}

						@Override
						public String next() {
							if (!hasNext()) {
								throw new IllegalStateException(
										"Last element reached!");
							}
							return String.valueOf(counter.incrementAndGet());
						}

						@Override
						public void remove() {
							throw new AssertionError("not implemented!");

						}

					};
				}
			};
		}

	}

}
