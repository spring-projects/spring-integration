package org.springframework.integration.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public class SimpleMessageGroupTests {

	private final Object key = new Object();

	private SimpleMessageGroup group = new SimpleMessageGroup(Collections.<Message<?>> emptyList(), key);

	@SuppressWarnings("unchecked")
	public void prepareForSequenceAwareMessageGroup() throws Exception{
		Class<SimpleMessageGroup> clazz =
				(Class<SimpleMessageGroup>)Class.forName("org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler$SequenceAwareMessageGroup");
		Constructor<SimpleMessageGroup> ctr = clazz.getDeclaredConstructor(MessageGroup.class);
		ctr.setAccessible(true);
		group = ctr.newInstance(group);
	}

	@Test
	public void shouldFindSupersedingMessagesIfSequenceAware() throws Exception{
		this.prepareForSequenceAwareMessageGroup();
		final Message<?> message1 = MessageBuilder.withPayload("test").setSequenceNumber(1).build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).setSequenceNumber(1).build();
		assertThat(group.canAdd(message1), is(true));
		group.add(message1);
		group.add(message2);
		assertThat(group.canAdd(message1), is(false));
	}

	@Test
	public void shouldIgnoreMessagesWithZeroSequenceNumberIfSequenceAware() throws Exception{
		this.prepareForSequenceAwareMessageGroup();
		final Message<?> message1 = MessageBuilder.withPayload("test").build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).build();
		assertThat(group.canAdd(message1), is(true));
		group.add(message1);
		group.add(message2);
		assertThat(group.canAdd(message1), is(true));
	}

	@Test // shoudl not fail with NPE (see INT-2666)
	public void shouldIgnoreNullValuesWhenInitializedWithCollectionContainingNulls() throws Exception{
		Message<?> m1 = mock(Message.class);
		Message<?> m2 = mock(Message.class);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		messages.add(m1);
		messages.add(null);
		messages.add(m2);
		SimpleMessageGroup grp = new SimpleMessageGroup(messages, 1);
		assertEquals(2, grp.getMessages().size());
	}
}
