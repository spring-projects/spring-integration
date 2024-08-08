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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.router.RecipientListRouter.Recipient;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Liujiong
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
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

	@BeforeEach
	public void aa() {
		context.start();
	}

	@Test
	public void testAddRecipient() {
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel2", "true"))
						.build());
		Message<?> message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertThat(chanel2.receive(0).getPayload()).isEqualTo(1);
	}

	@Test
	public void testAddRecipientWithNullExpression() {
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel3"))
						.build());

		Message<?> message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel3 = (PollableChannel) context.getBean("channel3");
		assertThat(chanel3.receive(0).getPayload()).isEqualTo(1);
	}

	@Test
	public void testRemoveRecipient() {
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel1"))
						.build());
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel4"))
						.build());
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.removeRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel4"))
						.build());

		Message<?> message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel4 = (PollableChannel) context.getBean("channel4");
		assertThat(chanel1.receive(0).getPayload()).isEqualTo(1);
		assertThat(chanel4.receive(0)).isNull();
	}

	@Test
	public void testRemoveRecipientWithExpression() {
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel1", "true"))
						.build());
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel5", "true"))
						.build());
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.removeRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel5", "true"))
						.build());

		Message<?> message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel5 = (PollableChannel) context.getBean("channel5");
		assertThat(chanel1.receive(0).getPayload()).isEqualTo(1);
		assertThat(chanel5.receive(0)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetRecipients() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.send(input,
				MessageBuilder.withPayload("'simpleRouter.handler'.addRecipient")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("channel1"))
						.build());
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.getRecipients()");
		PollableChannel channel1 = (PollableChannel) context.getBean("channel1");
		Message<?> result = this.output.receive(0);
		Collection<Recipient> mappings = (Collection<Recipient>) result.getPayload();
		assertThat(mappings.iterator().next().getChannel()).isEqualTo(channel1);
	}

	@Test
	public void testSetRecipients() {
		Map<String, String> map = new HashMap<>();
		map.put("channel6", "true");
		Message<?> message =
				MessageBuilder.withPayload("'simpleRouter.handler'.setRecipientMappings")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of(map))
						.build();
		this.input.send(message);
		message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel6 = (PollableChannel) context.getBean("channel6");
		assertThat(chanel6.receive(0).getPayload()).isEqualTo(1);
	}

	@Test
	public void testReplaceRecipients() {
		Properties newMapping = new Properties();
		newMapping.setProperty("channel7", "true");
		this.input.send(
				MessageBuilder.withPayload("'simpleRouter.handler'.replaceRecipients")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of(newMapping))
						.build());
		Message<?> message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel7 = (PollableChannel) context.getBean("channel7");
		assertThat(chanel7.receive(0).getPayload()).isEqualTo(1);
	}

}
