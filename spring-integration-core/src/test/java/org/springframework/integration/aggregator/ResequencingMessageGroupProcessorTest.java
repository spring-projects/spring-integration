package org.springframework.integration.aggregator;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 */
public class ResequencingMessageGroupProcessorTest {

	private ResequencingMessageGroupProcessor processor = new ResequencingMessageGroupProcessor();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void shouldProcessSequence() {
		Message prototypeMessage = MessageBuilder.withPayload("foo").setCorrelationId("x").setSequenceNumber(1).setSequenceSize(3).build();
		List<Message<?>> unmarked= new ArrayList<Message<?>>();
		Message message1 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(1).build();
		Message message2 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(2).build();
		Message message3 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(3).build();
		unmarked.add(message1);
		unmarked.add(message2);
		unmarked.add(message3);
		SimpleMessageGroup group = new SimpleMessageGroup(unmarked,"x");
		List<Message> processedMessages = (List<Message>) processor.processMessageGroup(group);
		assertThat(processedMessages, hasItems(message1, message2, message3));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void shouldPartiallProcessIncompleteSequence() {
		Message prototypeMessage = MessageBuilder.withPayload("foo").setCorrelationId("x").setSequenceNumber(1).setSequenceSize(4).build();
		List<Message<?>> unmarked= new ArrayList<Message<?>>();
		Message message2 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(4).build();
		Message message1 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(1).build();
		Message message3 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(3).build();
		unmarked.add(message1);
		unmarked.add(message2);
		unmarked.add(message3);
		SimpleMessageGroup group = new SimpleMessageGroup(unmarked,"x");
		List<Message> processedMessages = (List<Message>) processor.processMessageGroup(group);
		assertThat(processedMessages, hasItems(message1));
		assertThat(processedMessages.size(), is(1));
	}
}
