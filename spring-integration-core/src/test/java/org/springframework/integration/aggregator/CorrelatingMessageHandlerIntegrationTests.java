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

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class CorrelatingMessageHandlerIntegrationTests {

	private final MessageGroupStore store = new SimpleMessageStore(100);

	private final MessageChannel outputChannel = mock(MessageChannel.class);

	private final MessageGroupProcessor processor = new SimpleMessageGroupProcessor();

	private final AggregatingMessageHandler defaultHandler = new AggregatingMessageHandler(processor, store);

	@Before
	public void setupHandler() {
		when(outputChannel.send(isA(Message.class))).thenReturn(true);
		defaultHandler.setOutputChannel(outputChannel);
		defaultHandler.setSendTimeout(-1);
		defaultHandler.setBeanFactory(mock(BeanFactory.class));
		defaultHandler.afterPropertiesSet();
	}

	@Test
	public void completesSingleMessage() throws Exception {
		Message<?> message = correlatedMessage(1, 1, 1);
		defaultHandler.handleMessage(message);
		verify(outputChannel).send(message);
	}

	@Test
	public void completesAfterThreshold() throws Exception {
		defaultHandler.setReleaseStrategy(new MessageCountReleaseStrategy());
		MessageChannel discardChannel = mock(MessageChannel.class);
		when(discardChannel.send(any(Message.class))).thenReturn(true);
		defaultHandler.setDiscardChannel(discardChannel);
		Message<?> message1 = correlatedMessage(1, 2, 1);
		Message<?> message2 = correlatedMessage(1, 2, 2);
		defaultHandler.handleMessage(message1);
		verify(outputChannel).send(message1);
		defaultHandler.handleMessage(message2);
		verify(outputChannel, never()).send(message2);
		verify(discardChannel).send(message2);
	}

	@Test
	public void completesIfNoSequence() throws Exception {
		defaultHandler.setReleaseStrategy(new MessageCountReleaseStrategy(2));
		Message<?> message1 = MessageBuilder.withPayload(1).setCorrelationId("foo").build();
		Message<?> message2 = MessageBuilder.withPayload(2).setCorrelationId("foo").build();
		Message<?> message3 = MessageBuilder.withPayload(3).setCorrelationId("foo").build();
		defaultHandler.handleMessage(message1);
		verify(outputChannel, never()).send(message3);
		defaultHandler.handleMessage(message2);
		verify(outputChannel).send(message2);
		defaultHandler.handleMessage(message3);
		verify(outputChannel, never()).send(message3);
	}

	@Test
	public void completesWithoutReleasingIncompleteCorrelations() throws Exception {
		Message<?> message1 = correlatedMessage(1, 2, 1);
		Message<?> message2 = correlatedMessage(2, 2, 1);
		Message<?> message1a = correlatedMessage(1, 2, 2);
		Message<?> message2a = correlatedMessage(2, 2, 2);
		defaultHandler.handleMessage(message1);
		defaultHandler.handleMessage(message2);
		verify(outputChannel, never()).send(message1);
		verify(outputChannel, never()).send(message2);
		defaultHandler.handleMessage(message1a);
		verify(outputChannel).send(message1);
		verify(outputChannel).send(message1a);
		verify(outputChannel, never()).send(message2);
		verify(outputChannel, never()).send(message2a);
		defaultHandler.handleMessage(message2a);
		verify(outputChannel).send(message2);
		verify(outputChannel).send(message2a);
	}

	@Test
	public void completesAfterSequenceComplete() throws Exception {
		Message<?> message1 = correlatedMessage(1, 2, 1);
		Message<?> message2 = correlatedMessage(1, 2, 2);
		defaultHandler.handleMessage(message1);
		verify(outputChannel, never()).send(message1);
		defaultHandler.handleMessage(message2);
		verify(outputChannel).send(message1);
		verify(outputChannel).send(message2);
	}

	private Message<?> correlatedMessage(Object correlationId, Integer sequenceSize, Integer sequenceNumber) {
		return MessageBuilder.withPayload("test")
				.setCorrelationId(correlationId)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.build();
	}

}
