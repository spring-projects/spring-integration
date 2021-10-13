/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.jms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 *
 * @since 4.3
 */
@SpringJUnitConfig
@DirtiesContext
public class SplitterAggregatorTests extends ActiveMQMultiContextTests {

	@Autowired
	private MessageChannel splitChannel;

	@Autowired
	private PollableChannel resultChannel;

	@SuppressWarnings("unchecked")
	@Test
	public void testSplitterAggregatorOverJms() {
		List<Integer> payload = Arrays.asList(1, 2, 3, 4, 5, 6);
		this.splitChannel.send(new GenericMessage<>(payload));
		Message<?> message = this.resultChannel.receive(10000);
		assertThat(message).isNotNull();
		Collections.sort(((List<Integer>) message.getPayload()));
		assertThat(message.getPayload()).isEqualTo(payload);
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		@Splitter(inputChannel = "splitChannel")
		public MessageHandler splitter() {
			DefaultMessageSplitter splitter = new DefaultMessageSplitter();
			splitter.setOutputChannelName("toJmsChannel");
			return splitter;
		}

		@Bean
		@ServiceActivator(inputChannel = "toJmsChannel")
		public MessageHandler toJms() {
			JmsSendingMessageHandler handler = new JmsSendingMessageHandler(new JmsTemplate(connectionFactory));
			handler.setDestinationName("splitterAggregator");
			return handler;
		}

		@Bean
		@InboundChannelAdapter(value = "aggregateChannel",
				poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "10"))
		public MessageSource<Object> fromJms() {
			JmsDestinationPollingSource source = new JmsDestinationPollingSource(new JmsTemplate(connectionFactory));
			source.setDestinationName("splitterAggregator");
			return source;
		}

		@Bean
		public MessageChannel aggregateChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "aggregateChannel")
		public MessageHandler aggregator() {
			AggregatingMessageHandler handler =
					new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
			handler.setOutputChannel(resultChannel());
			return handler;
		}

		@Bean
		public PollableChannel resultChannel() {
			return new QueueChannel();
		}

	}

}
