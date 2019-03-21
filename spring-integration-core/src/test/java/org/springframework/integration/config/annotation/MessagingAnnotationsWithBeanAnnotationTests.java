/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.integration.config.annotation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
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
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
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
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 *
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

	@Autowired
	private MessageChannel functionServiceChannel;

	@Autowired
	private MessageChannel functionMessageServiceChannel;

	@Autowired
	private MessageChannel consumerServiceChannel;

	@Autowired
	private List<String> stringCollector;

	@Autowired
	private MessageChannel messageConsumerServiceChannel;

	@Test
	public void testMessagingAnnotationsFlow() {
		Stream.of(this.sourcePollingChannelAdapters).forEach(AbstractEndpoint::start);
		//this.sourcePollingChannelAdapter.start();
		for (int i = 0; i < 10; i++) {
			Message<?> receive = this.discardChannel.receive(10000);
			assertNotNull(receive);
			assertEquals(0, ((Integer) receive.getPayload()) % 2);

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
		for (Message<?> message : this.collector) {
			assertNotEquals(0, ((Integer) message.getPayload()) % 2);
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

		QueueChannel replyChannel = new QueueChannel();

		Message<String> message = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build();

		this.functionServiceChannel.send(message);

		Message<?> receive = replyChannel.receive(10_000);

		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		message = MessageBuilder.withPayload("BAR")
				.setReplyChannel(replyChannel)
				.build();

		this.functionMessageServiceChannel.send(message);

		receive = replyChannel.receive(10_000);

		assertNotNull(receive);
		assertEquals("bar", receive.getPayload());

		this.consumerServiceChannel.send(new GenericMessage<>("baz"));

		assertFalse(this.stringCollector.isEmpty());
		assertEquals("baz", this.stringCollector.iterator().next());

		this.collector.clear();

		this.messageConsumerServiceChannel.send(new GenericMessage<>("123"));

		assertFalse(this.collector.isEmpty());
		Message<?> next = this.collector.iterator().next();
		assertEquals("123", next.getPayload());
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

		@Bean(IntegrationContextUtils.MESSAGE_HANDLER_FACTORY_BEAN_NAME)
		public MessageHandlerMethodFactory messageHandlerMethodFactory() {
			return new DefaultMessageHandlerMethodFactory();
		}

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
			return counter::incrementAndGet;
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
			return new ArrayList<>();
		}

		@Bean
		public MessageChannel serviceChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "serviceChannel")
		public MessageHandler service() {
			return collector()::add;
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


		@Bean
		@Transformer(inputChannel = "functionServiceChannel")
		public Function<String, String> functionAsService() {
			return String::toUpperCase;
		}

		@Bean
		@ServiceActivator(inputChannel = "functionMessageServiceChannel")
		public Function<Message<String>, String> messageFunctionAsService() {
			return new Function<Message<String>, String>() { // Has to be interface for proper type inferring

				@Override
				public String apply(Message<String> m) {
					return m.getPayload().toLowerCase();
				}

			};
		}

		@Bean
		public List<String> stringCollector() {
			return new ArrayList<>();
		}

		@Bean
		@ServiceActivator(inputChannel = "consumerServiceChannel")
		public Consumer<String> consumerAsService() {
			return stringCollector()::add;
		}

		@Bean
		@ServiceActivator(inputChannel = "messageConsumerServiceChannel")
		public Consumer<Message<?>> messageConsumerAsService() {
			return new Consumer<Message<?>>() { // Has to be interface for proper type inferring

				@Override
				public void accept(Message<?> e) {
					collector().add(e);
				}

			};
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
