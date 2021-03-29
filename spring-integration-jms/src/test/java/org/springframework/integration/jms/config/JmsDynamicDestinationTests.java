/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.jms.TextMessage;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsDynamicDestinationTests extends ActiveMQMultiContextTests {

	@Autowired
	private MessageChannel channelAdapterChannel;

	@Autowired
	private MessageChannel gatewayChannel;

	@Autowired
	private PollableChannel channelAdapterResults1;

	@Autowired
	private PollableChannel channelAdapterResults2;

	@Test
	public void channelAdapter() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test-1").setHeader("destinationNumber", 1).build();
		Message<?> message2 = MessageBuilder.withPayload("test-2").setHeader("destinationNumber", 2).build();
		channelAdapterChannel.send(message1);
		channelAdapterChannel.send(message2);
		Message<?> result1 = channelAdapterResults1.receive(5000);
		Message<?> result2 = channelAdapterResults2.receive(5000);
		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		TextMessage jmsResult1 = (TextMessage) result1.getPayload();
		TextMessage jmsResult2 = (TextMessage) result2.getPayload();
		assertThat(jmsResult1.getText()).isEqualTo("test-1");
		assertThat(jmsResult1.getJMSDestination().toString()).isEqualTo("queue://queue.test.dynamic.adapter.1");
		assertThat(jmsResult2.getText()).isEqualTo("test-2");
		assertThat(jmsResult2.getJMSDestination().toString()).isEqualTo("queue://queue.test.dynamic.adapter.2");
	}

	@Test
	public void gateway() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test-1").setHeader("destinationNumber", 1).build();
		Message<?> message2 = MessageBuilder.withPayload("test-2").setHeader("destinationNumber", 2).build();
		MessagingTemplate template = new MessagingTemplate();
		Message<?> result1 = template.sendAndReceive(gatewayChannel, message1);
		Message<?> result2 = template.sendAndReceive(gatewayChannel, message2);
		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test-1!");
		assertThat(result2.getPayload()).isEqualTo("test-2!!");
	}


	public static class Responder {

		public String one(String message) {
			return message + "!";
		}

		public String two(String message) {
			return message + "!!";
		}

	}

}
