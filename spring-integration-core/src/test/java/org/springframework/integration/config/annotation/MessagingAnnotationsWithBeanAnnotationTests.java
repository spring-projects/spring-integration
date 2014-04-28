/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.PassThroughMessageGroupProcessor;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MessagingAnnotationsWithBeanAnnotationTests {

	@Autowired
	private SourcePollingChannelAdapter sourcePollingChannelAdapter;

	@Autowired
	private PollableChannel discardChannel;

	@Resource(name="collector")
	private List<Message<?>> collector;

	@Test
	public void testMessagingAnnotationsFlow() {
		this.sourcePollingChannelAdapter.start();
		for (int i = 0; i < 10; i++) {
			Message<?> receive = this.discardChannel.receive(1000);
			assertNotNull(receive);
			assertTrue(((Integer) receive.getPayload()) % 2 == 0);
		}
		for (Message<?> message : collector) {
			assertFalse(((Integer) message.getPayload()) % 2 == 0);
			MessageHistory messageHistory = MessageHistory.read(message);
			assertNotNull(messageHistory);
			String messageHistoryString = messageHistory.toString();
			assertThat(messageHistoryString, Matchers.containsString("routerChannel"));
			assertThat(messageHistoryString, Matchers.containsString("filterChannel"));
			assertThat(messageHistoryString, Matchers.containsString("aggregatorChannel"));
			assertThat(messageHistoryString, Matchers.containsString("splitterChannel"));
			assertThat(messageHistoryString, Matchers.containsString("serviceChannel"));
			assertThat(messageHistoryString, Matchers.not(Matchers.containsString("discardChannel")));
		}
	}

	@Configuration
	@EnableIntegration
	@EnableMessageHistory
	public static class ContextConfiguration {

		private static final ExpressionParser PARSER = new SpelExpressionParser();

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		@InboundChannelAdapter(value = "routerChannel", autoStartup = "false",
				poller = @Poller(fixedRate = "10", maxMessagesPerPoll = "1"))
		public MessageSource<Integer> counterMessageSource(final AtomicInteger counter) {
			return new MessageSource<Integer>() {

				@Override
				public Message<Integer> receive() {
					return new GenericMessage<Integer>(counter.incrementAndGet());
				}
			};
		}

		@Bean
		public MessageChannel routerChannel() {
			return new DirectChannel();
		}

		@Bean
		@Router(inputChannel = "routerChannel", channelMappings = {"true=odd", "false=filter"}, suffix = "Channel")
		public MessageSelector router() {
			return new ExpressionEvaluatingSelector("payload % 2 == 0");
		}

		@Bean
		@Transformer(inputChannel = "oddChannel", outputChannel = "filterChannel")
		public ExpressionEvaluatingTransformer oddTransformer() {
			return new ExpressionEvaluatingTransformer(PARSER.parseExpression("payload / 2"));
		}

		@Bean
		public MessageChannel filterChannel() {
			return new DirectChannel();
		}

		@Bean
		@Filter(inputChannel = "filterChannel", outputChannel = "aggregatorChannel", discardChannel = "discardChannel")
		public MessageSelector filter() {
			return new ExpressionEvaluatingSelector("payload % 2 != 0");
		}

		@Bean
		public MessageChannel aggregatorChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "aggregatorChannel")
		public MessageHandler aggregator() {
			AggregatingMessageHandler handler = new AggregatingMessageHandler(new PassThroughMessageGroupProcessor());
			handler.setCorrelationStrategy(new ExpressionEvaluatingCorrelationStrategy("1"));
			handler.setReleaseStrategy(new ExpressionEvaluatingReleaseStrategy("size() == 10"));
			handler.setOutputChannelName("splitterChannel");
			return handler;
		}

		@Bean
		public MessageChannel splitterChannel() {
			return new DirectChannel();
		}

		@Bean
		@Splitter(inputChannel = "splitterChannel")
		public MessageHandler splitter() {
			DefaultMessageSplitter defaultMessageSplitter = new DefaultMessageSplitter();
			defaultMessageSplitter.setOutputChannelName("serviceChannel");
			return defaultMessageSplitter;
		}

		@Bean
		public PollableChannel discardChannel() {
			return new QueueChannel();
		}

		@Bean
		public List<Message<?>> collector() {
			return new ArrayList<Message<?>>();
		}

		@Bean
		public MessageChannel serviceChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "serviceChannel")
		public MessageHandler service() {
			final List<Message<?>> collector = this.collector();
			return new MessageHandler() {

				@Override
				public void handleMessage(Message<?> message) throws MessagingException {
					collector.add(message);
				}
			};
		}

	}

}
