/*
 * Copyright 2002-present the original author or authors.
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

import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.channel.SubscribableJmsChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
	public void testMessageHistory() throws JMSException {
		AbstractMessageListenerContainer mlContainer = mock();
		JmsTemplate template = new JmsTemplate(connectionFactory) {

			@Override
			protected void doSend(MessageProducer producer, jakarta.jms.Message message) {
			}

		};
		template.setDefaultDestination(new ActiveMQQueue("test_queue"));
		MessageConverter messageConverter = spy(new SimpleMessageConverter());
		template.setMessageConverter(messageConverter);
		SubscribableJmsChannel channel = new SubscribableJmsChannel(mlContainer, template);
		channel.setShouldTrack(true);
		channel.setBeanName("jmsChannel");
		Message<String> message = new GenericMessage<>("hello");

		channel.send(message);

		ArgumentCaptor<Message<?>> messageArgumentCaptor = ArgumentCaptor.captor();
		verify(messageConverter).toMessage(messageArgumentCaptor.capture(), any());

		Message<?> messageInTheChannel = messageArgumentCaptor.getValue();
		MessageHistory history = MessageHistory.read(messageInTheChannel);
		assertThat(history)
				.first()
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsEntry("name", "jmsChannel");
		channel.stop();
	}

	@Test
	public void testFullConfig() {
		SubscribableChannel channel = context.getBean("jmsChannel", SubscribableChannel.class);
		PollableChannel resultChannel = context.getBean("resultChannel", PollableChannel.class);
		channel.send(new GenericMessage<>("hello"));
		Message<?> resultMessage = resultChannel.receive(10000);
		MessageHistory history = MessageHistory.read(resultMessage);
		assertThat(history)
				.first()
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsEntry("name", "jmsChannel");
	}

}
