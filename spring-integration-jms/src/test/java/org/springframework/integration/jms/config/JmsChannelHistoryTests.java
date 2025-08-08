/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsChannelHistoryTests extends ActiveMQMultiContextTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void testMessageHistory() {
		AbstractMessageListenerContainer mlContainer = mock(AbstractMessageListenerContainer.class);
		JmsTemplate template = mock(JmsTemplate.class);
		SubscribableJmsChannel channel = new SubscribableJmsChannel(mlContainer, template);
		channel.setShouldTrack(true);
		channel.setBeanName("jmsChannel");
		Message<String> message = new GenericMessage<>("hello");

		doAnswer(invocation -> {
			Message<String> msg = invocation.getArgument(0);
			MessageHistory history = MessageHistory.read(msg);
			assertThat(history.get(0).contains("jmsChannel")).isTrue();
			return null;
		}).when(template).convertAndSend(Mockito.any(Message.class));
		channel.send(message);
		verify(template, times(1)).convertAndSend(Mockito.any(Message.class));
		channel.stop();
	}

	@Test
	public void testFullConfig() {
		SubscribableChannel channel = context.getBean("jmsChannel", SubscribableChannel.class);
		PollableChannel resultChannel = context.getBean("resultChannel", PollableChannel.class);
		channel.send(new GenericMessage<>("hello"));
		Message<?> resultMessage = resultChannel.receive(10000);
		MessageHistory history = MessageHistory.read(resultMessage);
		assertThat(history.get(0).contains("jmsChannel")).isTrue();
	}

}
