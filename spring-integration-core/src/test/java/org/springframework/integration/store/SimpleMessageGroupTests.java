/*
 * Copyright 2009-2024 the original author or authors.
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

package org.springframework.integration.store;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
public class SimpleMessageGroupTests {

	private final Object key = new Object();

	private final SimpleMessageGroup group = new SimpleMessageGroup(new ArrayList<>(), key);

	private MessageGroup sequenceAwareGroup;

	public void prepareForSequenceAwareMessageGroup() throws Exception {
		Class<?> clazz =
				Class.forName("org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler$SequenceAwareMessageGroup");
		Constructor<?> ctr = clazz.getDeclaredConstructor(MessageGroup.class);
		ctr.setAccessible(true);
		this.sequenceAwareGroup = (MessageGroup) ctr.newInstance(this.group);
	}

	@Test
	public void shouldFindSupersedingMessagesIfSequenceAware() throws Exception {
		prepareForSequenceAwareMessageGroup();
		final Message<?> message1 = MessageBuilder.withPayload("test").setSequenceNumber(1).build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).setSequenceNumber(1).build();
		assertThat(this.sequenceAwareGroup.canAdd(message1)).isTrue();
		this.group.add(message1);
		this.group.add(message2);
		prepareForSequenceAwareMessageGroup();
		assertThat(this.sequenceAwareGroup.canAdd(message1)).isFalse();
	}

	@Test
	public void shouldIgnoreMessagesWithZeroSequenceNumberIfSequenceAware() throws Exception {
		prepareForSequenceAwareMessageGroup();
		final Message<?> message1 = MessageBuilder.withPayload("test").build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).build();
		assertThat(this.sequenceAwareGroup.canAdd(message1)).isTrue();
		this.group.add(message1);
		this.group.add(message2);
		prepareForSequenceAwareMessageGroup();
		assertThat(this.sequenceAwareGroup.canAdd(message1)).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldIgnoreNullValuesWhenInitializedWithCollectionContainingNulls() {
		Message<?> m1 = mock(Message.class);
		willReturn(new MessageHeaders(mock(Map.class))).given(m1).getHeaders();
		Message<?> m2 = mock(Message.class);
		willReturn(new MessageHeaders(mock(Map.class))).given(m2).getHeaders();
		final List<Message<?>> messages = new ArrayList<>();
		messages.add(m1);
		messages.add(null);
		messages.add(m2);
		SimpleMessageGroup grp = new SimpleMessageGroup(messages, 1);
		assertThat(grp.getMessages().size()).isEqualTo(2);
	}

	@Test
	// This test used to take 2 min and half to run; now ~200 milliseconds.
	public void testPerformance() {
		Collection<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 100000; i++) {
			messages.add(new GenericMessage<Object>("foo"));
		}
		SimpleMessageGroup group = new SimpleMessageGroup(messages, this.key);
		StopWatch watch = new StopWatch();
		watch.start();
		Collection<Message<?>> messagesInGroup = group.getMessages();
		for (Message<?> message : messages) {
			assertThat(messagesInGroup.contains(message)).isTrue();
		}
		watch.stop();
		assertThat(watch.getTotalTimeMillis()).isLessThan(5000);
	}

}
