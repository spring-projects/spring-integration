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

package org.springframework.integration.config.xml;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
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
		assertThat(messageStore).isEqualTo(context.getBean("testMessageStore"));
	}

	@Test
	public void checkMessageStoreReferenceOnCheckOut() {
		ClaimCheckOutTransformer transformer = (ClaimCheckOutTransformer) new DirectFieldAccessor(
				new DirectFieldAccessor(checkout).getPropertyValue("handler")).getPropertyValue("transformer");
		MessageStore messageStore = (MessageStore)
				new DirectFieldAccessor(transformer).getPropertyValue("messageStore");
		assertThat(messageStore).isEqualTo(context.getBean("testMessageStore"));
	}

	@Test
	public void integrationTest() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		checkinChannel.send(message);
		Message<?> wiretapMessage = wiretap.receive(0);
		assertThat(wiretapMessage).isNotNull();
		UUID payload = (UUID) wiretapMessage.getPayload();
		assertThat(payload).isEqualTo(message.getHeaders().getId());
		Message<?> resultMessage = replyChannel.receive(0);
		assertThat(resultMessage).isNotNull();
		assertThat(resultMessage.getPayload()).isEqualTo("test");
		assertThat(this.sampleMessageStore.getMessage(payload)).isNotNull();
	}

	@Test
	public void integrationTestWithRemoval() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		checkinChannelA.send(message);
		Message<?> wiretapMessage = wiretap.receive(0);
		assertThat(wiretapMessage).isNotNull();
		UUID payload = (UUID) wiretapMessage.getPayload();
		assertThat(payload).isEqualTo(message.getHeaders().getId());
		Message<?> resultMessage = replyChannel.receive(0);
		assertThat(resultMessage).isNotNull();
		assertThat(resultMessage.getPayload()).isEqualTo("test");
		assertThat(this.sampleMessageStore.getMessage(payload)).isNull();
	}

}
