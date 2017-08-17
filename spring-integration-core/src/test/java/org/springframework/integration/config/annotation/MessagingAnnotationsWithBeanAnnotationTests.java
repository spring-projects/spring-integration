/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.SimpleMessageGroupProcessor;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
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
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 4.0
 */
@ContextConfiguration(classes = MessagingAnnotationsWithBeanAnnotationTests.ContextConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MessagingAnnotationsWithBeanAnnotationTests {

	@Autowired
	private SourcePollingChannelAdapter[] sourcePollingChannelAdapters;

	@Autowired
	private PollableChannel discardChannel;

	@Resource(name = "collector")
	private List<Message<?>> collector;

	@Autowired(required = false)
	@Qualifier("messagingAnnotationsWithBeanAnnotationTests.ContextConfiguration.skippedMessageHandler.serviceActivator")
	private EventDrivenConsumer skippedServiceActivator;

	@Autowired(required = false)
	@Qualifier("skippedMessageHandler")
	private MessageHandler skippedMessageHandler;

	@Autowired(required = false)
	@Qualifier("skippedChannel")
	private MessageChannel skippedChannel;

	@Autowired(required = false)
	@Qualifier("skippedChannel2")
	private MessageChannel skippedChannel2;

	@Autowired(required = false)
	@Qualifier("skippedMessageSource")
	private MessageSource<?> skippedMessageSource;

	@Autowired
	private PollableChannel counterErrorChannel;

	@Test
	public void testMessagingAnnotationsFlow() {
		Stream.of(this.sourcePollingChannelAdapters).forEach(a -> a.start());
		//this.sourcePollingChannelAdapter.start();
		for (int i = 0; i < 10; i++) {
			Message<?> receive = this.discardChannel.receive(10000);
			assertNotNull(receive);
			assertTrue(((Integer) receive.getPayload()) % 2 == 0);

			receive = this.counterErrorChannel.receive(10000);
			assertNotNull(receive);
			assertThat(receive, instanceOf(ErrorMessage.class));
			assertThat(receive.getPayload(), instanceOf(MessageRejectedException.class));
			MessageRejectedException exception = (MessageRejectedException) receive.getPayload();
			assertThat(exception.getMessage(),
					containsString("MessageFilter " +
							"'messagingAnnotationsWithBeanAnnotationTests.ContextConfiguration.filter.filter.handler'" +
							" rejected Message"));

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

		assertNull(this.skippedServiceActivator);
		assertNull(this.skippedMessageHandler);
		assertNull(this.skippedChannel);
		assertNull(this.skippedChannel2);
		assertNull(this.skippedMessageSource);
	}

	@Test
	public void testInvalidMessagingAnnotationsConfig() {
		try {
			new AnnotationConfigApplicationContext(InvalidContextConfiguration.class).close();
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));
			assertThat(e.getCause(), instanceOf(BeanDefinitionValidationException.class));
			assertThat(e.getMessage(), containsString("The attribute causing the ambiguity is: [applySequence]."));
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
				poller = @Poller(fixedRate = "10", maxMessagesPerPoll = "1", errorChannel = "counterErrorChannel"))
		public MessageSource<Integer> counterMessageSource(final AtomicInteger counter) {
			return () -> new GenericMessage<>(counter.incrementAndGet());
		}

		@Bean
		@InboundChannelAdapter(value = "routerChannel", autoStartup = "false",
				poller = @Poller(fixedRate = "10", maxMessagesPerPoll = "1", errorChannel = "counterErrorChannel"))
		public Supplier<Integer> counterMessageSupplier(final AtomicInteger counter) {
			return () -> counter.incrementAndGet();
		}

		@Bean
		public PollableChannel counterErrorChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel routerChannel() {
			return new DirectChannel();
		}

		@Bean
		@Router(inputChannel = "routerChannel", channelMappings = { "true=odd", "false=filter" }, suffix = "Channel")
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
		@Filter(inputChannel = "filterChannel",
				outputChannel = "aggregatorChannel",
				discardChannel = "discardChannel",
				throwExceptionOnRejection = "true")
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
			AggregatingMessageHandler handler = new AggregatingMessageHandler(new SimpleMessageGroupProcessor());
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
			return message -> collector.add(message);
		}

		@Bean
		@ServiceActivator(inputChannel = "skippedChannel")
		@Splitter(inputChannel = "skippedChannel2")
		@Router(inputChannel = "skippedChannel3")
		@Transformer(inputChannel = "skippedChannel4")
		@Filter(inputChannel = "skippedChannel5")
		@Profile("foo")
		public MessageHandler skippedMessageHandler() {
			return m -> {
			};
		}

		@Bean
		@BridgeFrom("skippedChannel6")
		@Profile("foo")
		public MessageChannel skippedChannel1() {
			return new DirectChannel();
		}

		@Bean
		@BridgeTo
		@Profile("foo")
		public MessageChannel skippedChannel2() {
			return new DirectChannel();
		}

		@Bean
		@InboundChannelAdapter("serviceChannel")
		@Profile("foo")
		public MessageSource<?> skippedMessageSource() {
			return () -> new GenericMessage<>("foo");
		}

	}

	@Configuration
	@EnableIntegration
	static class InvalidContextConfiguration {

		@Bean
		@Splitter(inputChannel = "splitterChannel", applySequence = "false")
		public MessageHandler splitter() {
			return new DefaultMessageSplitter();
		}

	}


}
