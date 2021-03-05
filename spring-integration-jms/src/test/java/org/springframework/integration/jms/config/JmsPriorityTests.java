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

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0.2
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsPriorityTests extends ActiveMQMultiContextTests {

	@Autowired
	private MessageChannel channelAdapterChannel;

	@Autowired
	private PollableChannel channelAdapterResults;

	@Autowired
	private MessageChannel gatewayChannel;

	@Test
	public void verifyPrioritySettingOnChannelAdapterUsedAsJmsPriorityIfNoHeader() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").build();
		channelAdapterChannel.send(message);
		Message<?> result = channelAdapterResults.receive(5000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof javax.jms.Message).isTrue();
		javax.jms.Message jmsMessage = (javax.jms.Message) result.getPayload();
		assertThat(jmsMessage.getJMSPriority()).isEqualTo(3);
	}

	@Test
	public void verifyPriorityHeaderUsedAsJmsPriorityWithChannelAdapter() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").setPriority(7).build();
		channelAdapterChannel.send(message);
		Message<?> result = channelAdapterResults.receive(5000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof javax.jms.Message).isTrue();
		javax.jms.Message jmsMessage = (javax.jms.Message) result.getPayload();
		assertThat(jmsMessage.getJMSPriority()).isEqualTo(7);
	}

	@Test
	public void verifyPrioritySettingOnGatewayUsedAsJmsPriorityIfNoHeader() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		gatewayChannel.send(message);
		Message<?> result = replyChannel.receive(5000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("priority=2");
	}

	@Test
	public void verifyPriorityHeaderUsedAsJmsPriorityWithGateway() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setPriority(8).setReplyChannel(replyChannel).build();
		gatewayChannel.send(message);
		Message<?> result = replyChannel.receive(5000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("priority=8");
	}


	public static class PriorityReader implements SessionAwareMessageListener<javax.jms.Message> {

		public void onMessage(javax.jms.Message request, Session session) throws JMSException {
			String text = "priority=" + request.getJMSPriority();
			TextMessage reply = session.createTextMessage(text);
			MessageProducer producer = session.createProducer(request.getJMSReplyTo());
			reply.setJMSCorrelationID(request.getJMSMessageID());
			producer.send(reply);
		}

	}

}
