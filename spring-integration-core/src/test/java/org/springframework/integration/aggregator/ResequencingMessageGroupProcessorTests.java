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

package org.springframework.integration.aggregator;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 */
public class ResequencingMessageGroupProcessorTests {

	private ResequencingMessageGroupProcessor processor = new ResequencingMessageGroupProcessor();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void shouldProcessSequence() {
		Message prototypeMessage = MessageBuilder.withPayload("foo").setCorrelationId("x").setSequenceNumber(1).setSequenceSize(3).build();
		List<Message<?>> messages= new ArrayList<Message<?>>();
		Message message1 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(1).build();
		Message message2 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(2).build();
		Message message3 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(3).build();
		messages.add(message1);
		messages.add(message2);
		messages.add(message3);
		SimpleMessageGroup group = new SimpleMessageGroup(messages,"x");
		List<Message> processedMessages = (List<Message>) processor.processMessageGroup(group);
		assertThat(processedMessages, hasItems(message1, message2, message3));
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void shouldPartiallProcessIncompleteSequence() {
		Message prototypeMessage = MessageBuilder.withPayload("foo").setCorrelationId("x").setSequenceNumber(1).setSequenceSize(4).build();
		List<Message<?>> messages= new ArrayList<Message<?>>();
		Message message2 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(4).build();
		Message message1 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(1).build();
		Message message3 = MessageBuilder.fromMessage(prototypeMessage).setSequenceNumber(3).build();
		messages.add(message1);
		messages.add(message2);
		messages.add(message3);
		SimpleMessageGroup group = new SimpleMessageGroup(messages,"x");
		List<Message> processedMessages = (List<Message>) processor.processMessageGroup(group);
		assertThat(processedMessages, hasItems(message1));
		assertThat(processedMessages.size(), is(1));
	}
}
