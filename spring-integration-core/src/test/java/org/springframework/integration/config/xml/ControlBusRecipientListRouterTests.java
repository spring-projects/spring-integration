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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.router.RecipientListRouter.Recipient;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Liujiong
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ControlBusRecipientListRouterTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	@Qualifier("routingChannelA")
	private MessageChannel channel;

	@Before
	public void aa() {
		context.start();
	}

	@Test
	public void testAddRecipient() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel2','true')");
		Message<?> message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertThat(chanel2.receive(0).getPayload().equals(1)).isTrue();
	}

	@Test
	public void testAddRecipientWithNullExpression() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel3')");

		Message<?> message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel3 = (PollableChannel) context.getBean("channel3");
		assertThat(chanel3.receive(0).getPayload().equals(1)).isTrue();
	}

	@Test
	public void testRemoveRecipient() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel1')");
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel4')");
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.removeRecipient('channel4')");

		Message<?> message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel4 = (PollableChannel) context.getBean("channel4");
		assertThat(chanel1.receive(0).getPayload().equals(1)).isTrue();
		assertThat(chanel4.receive(0)).isNull();
	}

	@Test
	public void testRemoveRecipientWithExpression() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel1','true')");
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel5','true')");
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.removeRecipient('channel5','true')");

		Message<?> message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel5 = (PollableChannel) context.getBean("channel5");
		assertThat(chanel1.receive(0).getPayload().equals(1)).isTrue();
		assertThat(chanel5.receive(0)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetRecipients() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel1')");
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.getRecipients()");
		PollableChannel channel1 = (PollableChannel) context.getBean("channel1");
		Message<?> result = this.output.receive(0);
		Collection<Recipient> mappings = (Collection<Recipient>) result.getPayload();
		assertThat(mappings.iterator().next().getChannel()).isEqualTo(channel1);
	}

	@Test
	public void testSetRecipients() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);

		Map<String, String> map = new HashMap<String, String>();
		map.put("channel6", "true");
		Message<?> message = MessageBuilder.withPayload("@'simpleRouter.handler'.setRecipientMappings(headers.recipientMap)").setHeader("recipientMap", map).build();
		this.input.send(message);
		message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel6 = (PollableChannel) context.getBean("channel6");
		assertThat(chanel6.receive(0).getPayload().equals(1)).isTrue();
	}

	@Test
	public void testReplaceRecipients() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);

		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.replaceRecipients('channel7=true')");
		Message<?> message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel7 = (PollableChannel) context.getBean("channel7");
		assertThat(chanel7.receive(0).getPayload().equals(1)).isTrue();
	}

}
