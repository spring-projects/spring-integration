/*
 * Copyright 2009-2016 the original author or authors.
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

package org.springframework.integration.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Artem Bilan
 */
public class SimpleMessageGroupTests {

	private final Object key = new Object();

	private SimpleMessageGroup group = new SimpleMessageGroup(Collections.<Message<?>>emptyList(), key);

	@SuppressWarnings("unchecked")
	public void prepareForSequenceAwareMessageGroup() throws Exception {
		Class<SimpleMessageGroup> clazz =
				(Class<SimpleMessageGroup>) Class.forName("org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler$SequenceAwareMessageGroup");
		Constructor<SimpleMessageGroup> ctr = clazz.getDeclaredConstructor(MessageGroup.class);
		ctr.setAccessible(true);
		group = ctr.newInstance(group);
	}

	@Test
	public void shouldFindSupersedingMessagesIfSequenceAware() throws Exception {
		this.prepareForSequenceAwareMessageGroup();
		final Message<?> message1 = MessageBuilder.withPayload("test").setSequenceNumber(1).build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).setSequenceNumber(1).build();
		assertThat(group.canAdd(message1), is(true));
		group.add(message1);
		group.add(message2);
		assertThat(group.canAdd(message1), is(false));
	}

	@Test
	public void shouldIgnoreMessagesWithZeroSequenceNumberIfSequenceAware() throws Exception {
		this.prepareForSequenceAwareMessageGroup();
		final Message<?> message1 = MessageBuilder.withPayload("test").build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).build();
		assertThat(group.canAdd(message1), is(true));
		group.add(message1);
		group.add(message2);
		assertThat(group.canAdd(message1), is(true));
	}

	@Test // should not fail with NPE (see INT-2666)
	public void shouldIgnoreNullValuesWhenInitializedWithCollectionContainingNulls() throws Exception {
		Message<?> m1 = mock(Message.class);
		Message<?> m2 = mock(Message.class);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		messages.add(m1);
		messages.add(null);
		messages.add(m2);
		SimpleMessageGroup grp = new SimpleMessageGroup(messages, 1);
		assertEquals(2, grp.getMessages().size());
	}

	@Test
	// This test used to take 2 min and half to run; now ~200 milliseconds.
	public void testPerformance_INT3846() {
		Collection<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 100000; i++) {
			messages.add(new GenericMessage<Object>("foo"));
		}
		SimpleMessageGroup group = new SimpleMessageGroup(messages, this.key);
		StopWatch watch = new StopWatch();
		watch.start();
		for (Message<?> message : messages) {
			group.getMessages().contains(message);
		}
		watch.stop();
		assertTrue(watch.getTotalTimeMillis() < 5000);
	}

}
