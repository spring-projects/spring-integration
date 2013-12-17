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

package org.springframework.integration.aggregator.integration;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Oleg Zhurakousky
 */
public class ResequencerIntegrationTests {

	@Test
	public void validateUnboundedResequencerLight(){
		ApplicationContext context = new ClassPathXmlApplicationContext("ResequencerIntegrationTest-context.xml",  ResequencerIntegrationTests.class);
		MessageChannel inputChannel = context .getBean("resequencerLightInput", MessageChannel.class);
		QueueChannel outputChannel = context .getBean("outputChannel", QueueChannel.class);
		EventDrivenConsumer edc = context.getBean("resequencerLight", EventDrivenConsumer.class);
		ResequencingMessageHandler handler = TestUtils.getPropertyValue(edc, "handler", ResequencingMessageHandler.class);
		MessageGroupStore store = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);

		Message<?> message1 = MessageBuilder.withPayload("1").setCorrelationId("A").setSequenceNumber(1).build();
		Message<?> message2 = MessageBuilder.withPayload("2").setCorrelationId("A").setSequenceNumber(2).build();
		Message<?> message3 = MessageBuilder.withPayload("3").setCorrelationId("A").setSequenceNumber(3).build();
		Message<?> message4 = MessageBuilder.withPayload("4").setCorrelationId("A").setSequenceNumber(4).build();
		Message<?> message5 = MessageBuilder.withPayload("5").setCorrelationId("A").setSequenceNumber(5).build();
		Message<?> message6 = MessageBuilder.withPayload("6").setCorrelationId("A").setSequenceNumber(6).build();

		inputChannel.send(message3);
		assertNull(outputChannel.receive(0));

		inputChannel.send(message1);
		message1 = outputChannel.receive(0);
		assertNotNull(message1);
		assertEquals((Integer)1, new IntegrationMessageHeaderAccessor(message1).getSequenceNumber());

		inputChannel.send(message2);
		message2 = outputChannel.receive(0);
		message3 = outputChannel.receive(0);
		assertNotNull(message2);
		assertNotNull(message3);
		assertEquals((Integer)2, new IntegrationMessageHeaderAccessor(message2).getSequenceNumber());
		assertEquals((Integer)3, new IntegrationMessageHeaderAccessor(message3).getSequenceNumber());

		inputChannel.send(message5);
		assertNull(outputChannel.receive(0));

		inputChannel.send(message6);
		assertNull(outputChannel.receive(0));

		inputChannel.send(message4);
		message4 = outputChannel.receive(0);
		message5 = outputChannel.receive(0);
		message6 = outputChannel.receive(0);
		assertNotNull(message4);
		assertNotNull(message5);
		assertNotNull(message6);
		assertEquals((Integer)4, new IntegrationMessageHeaderAccessor(message4).getSequenceNumber());
		assertEquals((Integer)5, new IntegrationMessageHeaderAccessor(message5).getSequenceNumber());
		assertEquals((Integer)6, new IntegrationMessageHeaderAccessor(message6).getSequenceNumber());


		assertEquals(0, store.getMessageGroup("A").getMessages().size());
	}

	@Test
	public void validateUnboundedResequencerDeep(){
		ApplicationContext context = new ClassPathXmlApplicationContext("ResequencerIntegrationTest-context.xml",  ResequencerIntegrationTests.class);
		MessageChannel inputChannel = context .getBean("resequencerDeepInput", MessageChannel.class);
		QueueChannel outputChannel = context .getBean("outputChannel", QueueChannel.class);
		EventDrivenConsumer edc = context.getBean("resequencerDeep", EventDrivenConsumer.class);
		ResequencingMessageHandler handler = TestUtils.getPropertyValue(edc, "handler", ResequencingMessageHandler.class);
		MessageGroupStore store = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);

		Message<?> message1 = MessageBuilder.withPayload("1").setCorrelationId("A").setSequenceNumber(1).build();
		Message<?> message2 = MessageBuilder.withPayload("2").setCorrelationId("A").setSequenceNumber(2).build();
		Message<?> message3 = MessageBuilder.withPayload("3").setCorrelationId("A").setSequenceNumber(3).build();

		inputChannel.send(message3);
		assertNull(outputChannel.receive(0));
		inputChannel.send(message1);
		assertNotNull(outputChannel.receive(0));
		inputChannel.send(message2);
		assertNotNull(outputChannel.receive(0));
		assertNotNull(outputChannel.receive(0));
		assertEquals(0, store.getMessageGroup("A").getMessages().size());
	}
}
