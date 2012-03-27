package org.springframework.integration.store;

import java.lang.reflect.Constructor;
import java.util.Collections;

import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public class SimpleMessageGroupTests {

	private Object key = new Object();

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
}
