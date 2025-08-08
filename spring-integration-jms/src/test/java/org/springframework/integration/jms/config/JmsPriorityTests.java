/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
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

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(result.getPayload() instanceof jakarta.jms.Message).isTrue();
		jakarta.jms.Message jmsMessage = (jakarta.jms.Message) result.getPayload();
		assertThat(jmsMessage.getJMSPriority()).isEqualTo(3);
	}

	@Test
	public void verifyPriorityHeaderUsedAsJmsPriorityWithChannelAdapter() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").setPriority(7).build();
		channelAdapterChannel.send(message);
		Message<?> result = channelAdapterResults.receive(5000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof jakarta.jms.Message).isTrue();
		jakarta.jms.Message jmsMessage = (jakarta.jms.Message) result.getPayload();
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

	public static class PriorityReader implements SessionAwareMessageListener<jakarta.jms.Message> {

		public void onMessage(jakarta.jms.Message request, Session session) throws JMSException {
			String text = "priority=" + request.getJMSPriority();
			TextMessage reply = session.createTextMessage(text);
			MessageProducer producer = session.createProducer(request.getJMSReplyTo());
			reply.setJMSCorrelationID(request.getJMSMessageID());
			producer.send(reply);
		}

	}

}
