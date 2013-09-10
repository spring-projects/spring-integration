/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ClaimCheckParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel checkinChannel;

	@Autowired
	private MessageChannel checkinChannelA;

	@Autowired
	private PollableChannel wiretap;

	@Autowired
	private EventDrivenConsumer checkin;

	@Autowired
	private EventDrivenConsumer checkout;

	@Autowired
	private MessageStore sampleMessageStore;


	@Test
	public void checkMessageStoreReferenceOnCheckIn() {
		ClaimCheckInTransformer transformer = (ClaimCheckInTransformer) new DirectFieldAccessor(
				new DirectFieldAccessor(checkin).getPropertyValue("handler")).getPropertyValue("transformer");
		MessageStore messageStore = (MessageStore)
				new DirectFieldAccessor(transformer).getPropertyValue("messageStore");
		assertEquals(context.getBean("testMessageStore"), messageStore);
	}

	@Test
	public void checkMessageStoreReferenceOnCheckOut() {
		ClaimCheckOutTransformer transformer = (ClaimCheckOutTransformer) new DirectFieldAccessor(
				new DirectFieldAccessor(checkout).getPropertyValue("handler")).getPropertyValue("transformer");
		MessageStore messageStore = (MessageStore)
				new DirectFieldAccessor(transformer).getPropertyValue("messageStore");
		assertEquals(context.getBean("testMessageStore"), messageStore);
	}

	@Test
	public void integrationTest() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		checkinChannel.send(message);
		Message<?> wiretapMessage = wiretap.receive(0);
		assertNotNull(wiretapMessage);
		UUID payload = (UUID) wiretapMessage.getPayload();
		assertEquals(message.getHeaders().getId(), payload);
		Message<?> resultMessage = replyChannel.receive(0);
		assertNotNull(resultMessage);
		assertEquals("test", resultMessage.getPayload());
		assertNotNull(this.sampleMessageStore.getMessage(payload));
	}

	@Test
	public void integrationTestWithRemoval() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		checkinChannelA.send(message);
		Message<?> wiretapMessage = wiretap.receive(0);
		assertNotNull(wiretapMessage);
		UUID payload = (UUID) wiretapMessage.getPayload();
		assertEquals(message.getHeaders().getId(), payload);
		Message<?> resultMessage = replyChannel.receive(0);
		assertNotNull(resultMessage);
		assertEquals("test", resultMessage.getPayload());
		assertNull(this.sampleMessageStore.getMessage(payload));
	}

}
