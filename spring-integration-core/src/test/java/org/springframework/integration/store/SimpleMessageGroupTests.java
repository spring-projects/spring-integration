package org.springframework.integration.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public class SimpleMessageGroupTests {

	private Object key = new Object();

	private SimpleMessageGroup group = new SimpleMessageGroup(Collections.<Message<?>> emptyList(), key);

	@Test
	public void shouldFindSupersedingMessages() {
		final Message<?> message1 = MessageBuilder.withPayload("test").setSequenceNumber(1).build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).setSequenceNumber(1).build();
		assertThat(group.canAdd(message1), is(true));
		group.add(message1);
		group.add(message2);
		assertThat(group.canAdd(message1), is(false));
	}

	@Test
	public void shouldIgnoreMessagesWithZeroSequenceNumber() {
		final Message<?> message1 = MessageBuilder.withPayload("test").build();
		final Message<?> message2 = MessageBuilder.fromMessage(message1).build();
		assertThat(group.canAdd(message1), is(true));
		group.add(message1);
		group.add(message2);
		assertThat(group.canAdd(message1), is(true));
	}
}
