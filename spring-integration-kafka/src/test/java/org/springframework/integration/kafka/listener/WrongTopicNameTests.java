/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.listener;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.TopicNotFoundException;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Marius Bogoevici
 */
public class WrongTopicNameTests extends AbstractMessageListenerContainerTests {

	@Rule
	public final KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaRule getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test(expected = TopicNotFoundException.class)
	@SuppressWarnings("serial")
	public void testWrongTopicNameFails() throws Exception {
		createTopic(TEST_TOPIC, 5, 1, 1);

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();

		final KafkaMessageListenerContainer kafkaMessageListenerContainer
				= new KafkaMessageListenerContainer(connectionFactory, "WRONG-TOPIC");
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);

		KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter(kafkaMessageListenerContainer);

		StringDecoder decoder = new StringDecoder();
		kafkaMessageDrivenChannelAdapter.setKeyDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setPayloadDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setBeanFactory(mock(BeanFactory.class));
		kafkaMessageDrivenChannelAdapter.setOutputChannel(new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				fail();
				return true;
			}

			@Override
			public boolean send(Message<?> message, long timeout) {
				fail();
				return true;
			}
		});

		kafkaMessageDrivenChannelAdapter.afterPropertiesSet();
		kafkaMessageDrivenChannelAdapter.start();


	}

}
