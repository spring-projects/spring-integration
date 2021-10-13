/*
 * Copyright 2014-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

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
import org.springframework.integration.annotation.Reactive;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
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
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 *
 * @since 4.0
 */
@ContextConfiguration(classes = MessagingAnnotationsWithBeanAnnotationTests.ContextConfiguration.class)
@SpringJUnitConfig
@DirtiesContext
public class MessagingAnnotationsWithBeanAnnotationTests {

	@Autowired
	private SourcePollingChannelAdapter[] sourcePollingChannelAdapters;

	@Autowired
	private PollableChannel discardChannel;

	@Autowired
	private List<Message<?>> collector;

	@Autowired(required = false)
	@Qualifier(
			"messagingAnnotationsWithBeanAnnotationTests.ContextConfiguration.skippedMessageHandler.serviceActivator")
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

	@Autowired
	private CountDownLatch reactiveCustomizerLatch;

	@Test
	public void testMessagingAnnotationsFlow() throws InterruptedException {
		Stream.of(this.sourcePollingChannelAdapters).forEach(AbstractEndpoint::start);
		for (int i = 0; i < 10; i++) {
			Message<?> receive = this.discardChannel.receive(10000);
			assertThat(receive).isNotNull();
			assertThat(((Integer) receive.getPayload()) % 2).isEqualTo(0);

			receive = this.counterErrorChannel.receive(10000);
			assertThat(receive).isNotNull();
			assertThat(receive).isInstanceOf(ErrorMessage.class);
			assertThat(receive.getPayload()).isInstanceOf(MessageRejectedException.class);
			MessageRejectedException exception = (MessageRejectedException) receive.getPayload();
			assertThat(exception.getMessage())
					.contains("message has been rejected in filter: bean " +
							"'messagingAnnotationsWithBeanAnnotationTests.ContextConfiguration.filter.filter.handler'");

		}

		assertThat(reactiveCustomizerLatch.await(10, TimeUnit.SECONDS)).isTrue();

		for (Message<?> message : this.collector) {
			assertThat(((Integer) message.getPayload()) % 2).isNotEqualTo(0);
			MessageHistory messageHistory = MessageHistory.read(message);
			assertThat(messageHistory).isNotNull();
			String messageHistoryString = messageHistory.toString();
			assertThat(messageHistoryString).contains("routerChannel")
					.contains("filterChannel")
					.contains("aggregatorChannel")
					.contains("splitterChannel")
					.contains("serviceChannel")
					.doesNotContain("discardChannel");
		}

		assertThat(this.skippedServiceActivator).isNull();
		assertThat(this.skippedMessageHandler).isNull();
		assertThat(this.skippedChannel).isNull();
		assertThat(this.skippedChannel2).isNull();
		assertThat(this.skippedMessageSource).isNull();

		QueueChannel replyChannel = new QueueChannel();

		Message<String> message = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build();

		this.functionServiceChannel.send(message);

		Message<?> receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");

		message = MessageBuilder.withPayload("BAR")
				.setReplyChannel(replyChannel)
				.build();

		this.functionMessageServiceChannel.send(message);

		receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("bar");

		this.consumerServiceChannel.send(new GenericMessage<>("baz"));

		assertThat(this.stringCollector.isEmpty()).isFalse();
		assertThat(this.stringCollector.iterator().next()).isEqualTo("baz");

		this.collector.clear();

		this.messageConsumerServiceChannel.send(new GenericMessage<>("123"));

		assertThat(this.collector.isEmpty()).isFalse();
		Message<?> next = this.collector.iterator().next();
		assertThat(next.getPayload()).isEqualTo("123");
	}

	@Test
	public void testInvalidMessagingAnnotationsConfig() {
		assertThatExceptionOfType(BeanDefinitionValidationException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(InvalidContextConfiguration.class))
				.withMessageContaining("The attribute causing the ambiguity is: [applySequence].");
	}

	@Autowired
	private MessageChannel reactiveMessageHandlerChannel;

	@Autowired
	private ContextConfiguration contextConfiguration;

	@Test
	public void testReactiveMessageHandler() {
		this.reactiveMessageHandlerChannel.send(new GenericMessage<>("test"));

		StepVerifier.create(
				this.contextConfiguration.messageMono
						.asMono()
						.map(Message::getPayload)
						.cast(String.class))
				.expectNext("test")
				.verifyComplete();
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
		public CountDownLatch reactiveCustomizerLatch() {
			return new CountDownLatch(10);
		}

		@Bean
		public Function<Flux<?>, Flux<?>> reactiveCustomizer(CountDownLatch reactiveCustomizerLatch) {
			return flux -> flux.doOnNext(data -> reactiveCustomizerLatch.countDown());
		}

		@Bean
		@Splitter(inputChannel = "splitterChannel", reactive = @Reactive("reactiveCustomizer"))
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
			return (message) -> message.getPayload().toLowerCase();
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
			return collector()::add;
		}

		Sinks.One<Message<?>> messageMono = Sinks.one();

		@Bean
		MessageChannel reactiveMessageHandlerChannel() {
			return new FluxMessageChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "reactiveMessageHandlerChannel")
		public ReactiveMessageHandler reactiveMessageHandlerService() {
			return (message) -> {
				messageMono.tryEmitValue(message);
				return Mono.empty();
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
