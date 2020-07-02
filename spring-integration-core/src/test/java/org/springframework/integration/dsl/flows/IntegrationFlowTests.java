/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.dsl.flows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.After;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.PayloadSerializingTransformer;
import org.springframework.integration.util.NoBeansOverrideAnnotationConfigContextLoader;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Mono;

/**
 * @author Artem Bilan
 * @author Tim Ysewyn
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Tim Feuerbach
 *
 * @since 5.0
 */
@ContextConfiguration(loader = NoBeansOverrideAnnotationConfigContextLoader.class)
@SpringJUnitConfig
@DirtiesContext
public class IntegrationFlowTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private ControlBusGateway controlBus;

	@Autowired
	@Qualifier("inputChannel")
	private MessageChannel inputChannel;

	@Autowired
	@Qualifier("discardChannel")
	private PollableChannel discardChannel;

	@Autowired
	@Qualifier("foo")
	private SubscribableChannel foo;

	@Autowired
	@Qualifier("successChannel")
	private PollableChannel successChannel;

	@Autowired
	@Qualifier("suppliedChannel")
	private PollableChannel suppliedChannel;

	@Autowired
	@Qualifier("suppliedChannel2")
	private PollableChannel suppliedChannel2;

	@Autowired
	@Qualifier("bridgeFlowInput")
	private PollableChannel bridgeFlowInput;

	@Autowired
	@Qualifier("bridgeFlowOutput")
	private PollableChannel bridgeFlowOutput;

	@Autowired
	@Qualifier("bridgeFlow2Input")
	private MessageChannel bridgeFlow2Input;

	@Autowired
	@Qualifier("bridgeFlow2Output")
	private PollableChannel bridgeFlow2Output;

	@Autowired
	@Qualifier("methodInvokingInput")
	private MessageChannel methodInvokingInput;

	@Autowired
	@Qualifier("delayedAdvice")
	private DelayedAdvice delayedAdvice;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	@Qualifier("claimCheckInput")
	private MessageChannel claimCheckInput;

	@Autowired
	@Qualifier("lambdasInput")
	private MessageChannel lambdasInput;

	@Test
	public void testWithSupplierMessageSourceImpliedPoller() {
		assertThat(this.suppliedChannel.receive(10000).getPayload()).isEqualTo("FOO");
	}

	@Test
	public void testWithSupplierMessageSourceProvidedPoller() {
		assertThat(this.suppliedChannel2.receive(10000).getPayload()).isEqualTo("FOO");
	}

	@Test
	public void testDirectFlow() {
		assertThat(this.beanFactory.containsBean("filter")).isTrue();
		assertThat(this.beanFactory.containsBean("filter.handler")).isTrue();
		assertThat(this.beanFactory.containsBean("expressionFilter")).isTrue();
		assertThat(this.beanFactory.containsBean("expressionFilter.handler")).isTrue();
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("100").setReplyChannel(replyChannel).build();

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.inputChannel.send(message))
				.withCauseInstanceOf(MessageDispatchingException.class)
				.withMessageContaining("Dispatcher has no subscribers");

		this.controlBus.send("@payloadSerializingTransformer.start()");

		final AtomicBoolean used = new AtomicBoolean();

		this.foo.subscribe(m -> used.set(true));

		this.inputChannel.send(message);
		Message<?> reply = replyChannel.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo(200);

		Message<?> successMessage = this.successChannel.receive(10000);
		assertThat(successMessage).isNotNull();
		assertThat(successMessage.getPayload()).isEqualTo(100);

		assertThat(used.get()).isTrue();

		this.inputChannel.send(new GenericMessage<Object>(1000));
		Message<?> discarded = this.discardChannel.receive(10000);
		assertThat(discarded).isNotNull();
		assertThat(discarded.getPayload()).isEqualTo("Discarded: 1000");
	}

	@Test
	public void testBridge() {
		GenericMessage<String> message = new GenericMessage<>("test");
		this.bridgeFlowInput.send(message);
		Message<?> reply = this.bridgeFlowOutput.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("test");

		assertThat(this.beanFactory.containsBean("bridgeFlow2.channel#0")).isTrue();
		assertThat(this.beanFactory.getBean("bridgeFlow2.channel#0")).isInstanceOf(FixedSubscriberChannel.class);

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.bridgeFlow2Input.send(message))
				.withCauseInstanceOf(MessageDispatchingException.class)
				.withMessageContaining("Dispatcher has no subscribers");

		this.controlBus.send("@bridge.start()");
		this.bridgeFlow2Input.send(message);
		reply = this.bridgeFlow2Output.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("test");
		assertThat(this.delayedAdvice.getInvoked()).isTrue();
	}

	@Test
	public void testWrongLastMessageChannel() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(InvalidLastMessageChannelFlowContext.class))
				.withMessageContaining("'.fixedSubscriberChannel()' " +
						"can't be the last EIP-method in the 'IntegrationFlow' definition");
	}

	@Test
	public void testMethodInvokingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("world")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.methodInvokingInput.send(message);
		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Hello World and world");
	}

	@Test
	public void testLambdas() {
		assertThat(this.beanFactory.containsBean("lambdasFlow.filter#0")).isTrue();
		assertThat(this.beanFactory.containsBean("lambdasFlow.transformer#0")).isTrue();

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("World".getBytes())
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.lambdasInput.send(message);
		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Hello World");

		message = MessageBuilder.withPayload("Spring")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();

		this.lambdasInput.send(message);
		assertThat(replyChannel.receive(10)).isNull();

	}

	@Test
	public void testClaimCheck() {
		QueueChannel replyChannel = new QueueChannel();

		Message<String> message = MutableMessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		this.claimCheckInput.send(message);

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive).isSameAs(message);

		assertThat(this.messageStore.getMessageCount()).isEqualTo(1);
		assertThat(this.messageStore.getMessage(message.getHeaders().getId())).isSameAs(message);
	}

	@Autowired
	private SubscribableChannel tappedChannel1;

	@Autowired
	@Qualifier("wireTapFlow2.input")
	private SubscribableChannel tappedChannel2;

	@Autowired
	@Qualifier("wireTapFlow3.input")
	private SubscribableChannel tappedChannel3;

	@Autowired
	private SubscribableChannel tappedChannel4;

	@Autowired
	@Qualifier("tapChannel")
	private QueueChannel tapChannel;

	@Autowired
	@Qualifier("wireTapFlow5.input")
	private SubscribableChannel tappedChannel5;

	@Autowired
	private PollableChannel wireTapSubflowResult;

	@Test
	public void testWireTap() {
		this.tappedChannel1.send(new GenericMessage<>("foo"));
		this.tappedChannel1.send(new GenericMessage<>("bar"));
		Message<?> out = this.tapChannel.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(this.tapChannel.receive(0)).isNull();

		this.tappedChannel2.send(new GenericMessage<>("foo"));
		this.tappedChannel2.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(this.tapChannel.receive(0)).isNull();

		this.tappedChannel3.send(new GenericMessage<>("foo"));
		this.tappedChannel3.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(this.tapChannel.receive(0)).isNull();

		this.tappedChannel4.send(new GenericMessage<>("foo"));
		this.tappedChannel4.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foo");
		out = this.tapChannel.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("bar");

		this.tappedChannel5.send(new GenericMessage<>(""));
		out = this.wireTapSubflowResult.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("");
	}

	@Autowired
	@Qualifier("subscribersFlow.input")
	private MessageChannel subscribersFlowInput;

	@Autowired
	@Qualifier("subscriber1Results")
	private PollableChannel subscriber1Results;

	@Autowired
	@Qualifier("subscriber2Results")
	private PollableChannel subscriber2Results;

	@Autowired
	@Qualifier("subscriber3Results")
	private PollableChannel subscriber3Results;

	@Test
	public void testSubscribersSubFlows() {
		this.subscribersFlowInput.send(new GenericMessage<>(2));

		Message<?> receive1 = this.subscriber1Results.receive(10000);
		assertThat(receive1).isNotNull();
		assertThat(receive1.getPayload()).isEqualTo(1);

		Message<?> receive2 = this.subscriber2Results.receive(10000);
		assertThat(receive2).isNotNull();
		assertThat(receive2.getPayload()).isEqualTo(4);
		Message<?> receive3 = this.subscriber3Results.receive(10000);
		assertThat(receive3).isNotNull();
		assertThat(receive3.getPayload()).isEqualTo(6);
	}

	@Autowired
	@Qualifier("errorRecovererFunction")
	private Function<String, String> errorRecovererFlowGateway;

	@Test
	public void testReplyChannelFromReplyMessage() {
		assertThat(this.errorRecovererFlowGateway.apply("foo")).isEqualTo("foo");
	}

	@Autowired
	private MessageChannel dedicatedQueueChannel;

	@Autowired
	private SubscribableChannel dedicatedResults;

	@Test
	public void testDedicatedPollingThreadFlow() throws InterruptedException {
		AtomicReference<String> threadNameReference = new AtomicReference<>();
		CountDownLatch resultLatch = new CountDownLatch(1);
		this.dedicatedResults.subscribe(m -> {
			threadNameReference.set(Thread.currentThread().getName());
			resultLatch.countDown();
		});

		this.dedicatedQueueChannel.send(new GenericMessage<>("foo"));

		assertThat(resultLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(threadNameReference.get()).isEqualTo("dedicatedTaskScheduler-1");
	}

	@Autowired
	private MessageChannel flowWithNullChannelInput;

	@Autowired
	private NullChannel nullChannel;

	@Test
	public void testNullChannelInTheEndOfFlow() {
		this.flowWithNullChannelInput.send(new GenericMessage<>("foo"));
	}

	@Autowired
	@Qualifier("flowWithLocalNullChannel.input")
	private MessageChannel flowWithLocalNullChannelInput;

	@Autowired
	@Qualifier("flowWithLocalNullChannel.channel#0")
	private NullChannel localNullChannel;

	@Test
	public void testLocalNullChannel() {
		this.flowWithLocalNullChannelInput.send(new GenericMessage<>("foo"));
	}


	@Autowired
	private EventDrivenConsumer flow1WithPrototypeHandlerConsumer;

	@Autowired
	private EventDrivenConsumer flow2WithPrototypeHandlerConsumer;

	@Test
	public void testPrototypeIsNotOverridden() {
		assertThat(this.flow2WithPrototypeHandlerConsumer.getHandler())
				.isNotSameAs(this.flow1WithPrototypeHandlerConsumer.getHandler());
	}

	@Autowired
	@Qualifier("globalErrorChannelResolutionFunction")
	private Consumer<String> globalErrorChannelResolutionGateway;

	@Autowired
	SubscribableChannel errorChannel;

	@Test
	public void testGlobalErrorChannelResolutionFlow() throws InterruptedException {
		CountDownLatch errorMessageLatch = new CountDownLatch(1);
		MessageHandler errorMessageHandler = m -> errorMessageLatch.countDown();
		this.errorChannel.subscribe(errorMessageHandler);

		this.globalErrorChannelResolutionGateway.accept("foo");

		assertThat(errorMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();

		this.errorChannel.unsubscribe(errorMessageHandler);
	}

	@Autowired
	@Qualifier("interceptorChannelIn")
	private MessageChannel interceptorChannelIn;

	@Autowired
	private List<String> outputStringList;

	@Test
	public void testInterceptorFlow() {
		this.interceptorChannelIn.send(MessageBuilder.withPayload("foo").build());

		assertThat(outputStringList).containsExactly(
				"Pre send transform: foo",
				"Pre send handle: FOO",
				"Handle: FOO",
				"Post send handle: FOO",
				"Post send transform: foo"
		);
	}

	@Autowired
	@Qualifier("controlBusFlow")
	Lifecycle controlBusFlow;

	@Test
	public void testStandardIntegrationFlowLifecycle() {
		this.controlBusFlow.stop();

		GatewayProxyFactoryBean controlBusGateway =
				this.beanFactory.getBean("&controlBusGateway", GatewayProxyFactoryBean.class);
		assertThat(controlBusGateway.isRunning()).isFalse();
		Lifecycle controlBus = this.beanFactory.getBean("controlBus", Lifecycle.class);
		assertThat(controlBus.isRunning()).isFalse();

		this.controlBusFlow.start();

		assertThat(controlBusGateway.isRunning()).isTrue();
		assertThat(controlBus.isRunning()).isTrue();
	}


	@After
	public void cleanUpList() {
		outputStringList.clear();
	}


	@MessagingGateway
	public interface ControlBusGateway {

		void send(String command);

	}

	@Configuration
	@EnableIntegration
	public static class SupplierContextConfiguration1 {

		@Bean
		public Function<String, String> toUpperCaseFunction() {
			return String::toUpperCase;
		}

		@Bean
		public Supplier<String> stringSupplier() {
			return () -> "foo";
		}

		@Bean
		public IntegrationFlow supplierFlow() {
			return IntegrationFlows.fromSupplier(stringSupplier())
					.transform(toUpperCaseFunction())
					.channel("suppliedChannel")
					.get();
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata poller() {
			return Pollers.fixedRate(100).get();
		}

		@Bean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
		public TaskScheduler taskScheduler() {
			ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			threadPoolTaskScheduler.setPoolSize(100);
			return threadPoolTaskScheduler;
		}


		@Bean
		public MessageChannel suppliedChannel() {
			return MessageChannels.queue(10).get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class SupplierContextConfiguration2 {

		@Bean
		public IntegrationFlow supplierFlow2() {
			return IntegrationFlows.fromSupplier(() -> "foo",
					c -> c.poller(Pollers.fixedDelay(100).maxMessagesPerPoll(1)))
					.<String, String>transform(String::toUpperCase)
					.channel("suppliedChannel2")
					.get();
		}

		@Bean
		public MessageChannel suppliedChannel2() {
			return MessageChannels.queue(10).get();
		}

	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow controlBusFlow() {
			return IntegrationFlows.from(ControlBusGateway.class, (gateway) -> gateway.beanName("controlBusGateway"))
					.controlBus((endpoint) -> endpoint.id("controlBus"))
					.get();
		}

		@Bean
		public MessageChannel inputChannel() {
			return MessageChannels.direct().get();
		}

		@Bean
		public MessageChannel foo() {
			return MessageChannels.publishSubscribe().get();
		}

	}

	@Configuration
	@ComponentScan
	public static class ContextConfiguration2 {

		@Autowired
		@Qualifier("inputChannel")
		private MessageChannel inputChannel;

		@Autowired
		@Qualifier("successChannel")
		private PollableChannel successChannel;


		@Bean
		public Advice expressionAdvice() {
			ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
			advice.setSuccessChannel(this.successChannel);
			return advice;
		}

		@Bean
		public IntegrationFlow flow2() {
			return IntegrationFlows.from(this.inputChannel)
					.filter(p -> p instanceof String, e -> e
							.id("filter")
							.discardFlow(df -> df
									.transform(String.class, "Discarded: "::concat)
									.channel(MessageChannels.queue("discardChannel"))))
					.channel("foo")
					.fixedSubscriberChannel()
					.<String, Integer>transform(Integer::parseInt)
					.transform(Foo::new)
					.transform(new PayloadSerializingTransformer(),
							c -> c.autoStartup(false).id("payloadSerializingTransformer"))
					.channel(MessageChannels.queue(new SimpleMessageStore(), "fooQueue"))
					.transform(Transformers.deserializer(Foo.class.getName()))
					.<Foo, Integer>transform(f -> f.value)
					.filter("true", e -> e.id("expressionFilter"))
					.channel(publishSubscribeChannel())
					.transform((Integer p) -> p * 2, c -> c.advice(this.expressionAdvice()))
					.get();
		}

		@Bean
		public MessageChannel publishSubscribeChannel() {
			return MessageChannels.publishSubscribe().get();
		}

		@Bean
		public IntegrationFlow subscribersFlow() {
			return flow -> flow
					.publishSubscribeChannel(executor(), s -> s
							.subscribe(f -> f
									.<Integer>handle((p, h) -> p / 2)
									.channel(MessageChannels.queue("subscriber1Results")))
							.subscribe(f -> f
									.<Integer>handle((p, h) -> p * 2)
									.channel(MessageChannels.queue("subscriber2Results"))))
					.<Integer>handle((p, h) -> p * 3)
					.channel(MessageChannels.queue("subscriber3Results"));
		}

		@Bean
		public Executor executor() {
			ThreadPoolTaskExecutor tpte = new ThreadPoolTaskExecutor();
			tpte.setCorePoolSize(50);
			return tpte;
		}

		@Bean
		public MessageHandler loggingMessageHandler() {
			return new LoggingHandler(LoggingHandler.Level.DEBUG);
		}

		@Bean
		public IntegrationFlow wireTapFlow1() {
			return IntegrationFlows.from("tappedChannel1")
					.wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
					.handle(new ReactiveMessageHandlerAdapter((message) -> Mono.just(message).log().then()))
					.get();
		}

		@Bean
		public IntegrationFlow wireTapFlow2() {
			return f -> f
					.wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
					.handle(loggingMessageHandler());
		}

		@Bean
		public IntegrationFlow wireTapFlow3() {
			return f -> f
					.transform("payload")
					.wireTap("tapChannel", wt -> wt.selector("payload == 'foo'"))
					.handle(loggingMessageHandler());
		}

		@Bean
		public IntegrationFlow wireTapFlow4() {
			return IntegrationFlows.from("tappedChannel4")
					.wireTap(tapChannel())
					.channel("nullChannel")
					.get();
		}

		@Bean
		public IntegrationFlow wireTapFlow5() {
			return f -> f
					.wireTap(sf -> sf
							.transform(// Must not be lambda for SpEL fallback behavior on empty payload
									new GenericTransformer<String, String>() {

										@Override
										public String transform(String source) {
											return source.toUpperCase();
										}

									})
							.channel(MessageChannels.queue("wireTapSubflowResult")))
					.channel("nullChannel");
		}

		@Bean
		public QueueChannel tapChannel() {
			return new QueueChannel();
		}

	}

	@MessageEndpoint
	public static class AnnotationTestService {

		@ServiceActivator(inputChannel = "publishSubscribeChannel")
		public void handle(Object payload) {
			assertThat(payload).isEqualTo(100);
		}

	}

	@Configuration
	public static class ContextConfiguration3 {

		@Autowired
		@Qualifier("delayedAdvice")
		private MethodInterceptor delayedAdvice;

		@Bean
		public QueueChannel successChannel() {
			return MessageChannels.queue().get();
		}

		@Bean
		public IntegrationFlow bridgeFlow() {
			return IntegrationFlows.from(MessageChannels.queue("bridgeFlowInput"))
					.channel(MessageChannels.queue("bridgeFlowOutput"))
					.get();
		}

		@Bean
		public IntegrationFlow bridgeFlow2() {
			return IntegrationFlows.from("bridgeFlow2Input")
					.bridge(c -> c.autoStartup(false).id("bridge"))
					.fixedSubscriberChannel()
					.delay("delayer", d -> d
							.delayExpression("200")
							.advice(this.delayedAdvice)
							.messageStore(this.messageStore()))
					.channel(MessageChannels.queue("bridgeFlow2Output"))
					.get();
		}

		@Bean
		public SimpleMessageStore messageStore() {
			return new SimpleMessageStore();
		}

		@Bean
		public IntegrationFlow claimCheckFlow() {
			return IntegrationFlows.from("claimCheckInput")
					.claimCheckIn(this.messageStore())
					.claimCheckOut(this.messageStore())
					.get();
		}

	}

	@Component("delayedAdvice")
	public static class DelayedAdvice implements MethodInterceptor {

		private final AtomicBoolean invoked = new AtomicBoolean();

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			this.invoked.set(true);
			return invocation.proceed();
		}

		public Boolean getInvoked() {
			return invoked.get();
		}

	}

	@Configuration
	public static class ContextConfiguration4 {

		@Autowired
		@Qualifier("integrationFlowTests.GreetingService")
		private MessageHandler greetingService;

		@Bean
		public IntegrationFlow methodInvokingFlow() {
			return IntegrationFlows.from("methodInvokingInput")
					.handle(this.greetingService)
					.get();
		}

		@Bean
		public IntegrationFlow lambdasFlow() {
			return IntegrationFlows.from("lambdasInput")
					.filter(String.class, "World"::equals)
					.transform(String.class, "Hello "::concat)
					.get();
		}

		@Bean
		public IntegrationFlow errorRecovererFlow() {
			return IntegrationFlows.from(Function.class, (gateway) -> gateway.beanName("errorRecovererFunction"))
					.<Object>handle((p, h) -> {
								throw new RuntimeException("intentional");
							},
							e -> e.advice(retryAdvice()))
					.get();
		}

		@Bean
		public RequestHandlerRetryAdvice retryAdvice() {
			RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
			requestHandlerRetryAdvice.setRecoveryCallback(new ErrorMessageSendingRecoverer(recoveryChannel()));
			return requestHandlerRetryAdvice;
		}

		@Bean
		public MessageChannel recoveryChannel() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow recoveryFlow() {
			return IntegrationFlows.from(recoveryChannel())
					.<MessagingException, Message<?>>transform(MessagingException::getFailedMessage)
					.get();

		}

		@Bean
		public IntegrationFlow dedicatedPollingThreadFlow() {
			return IntegrationFlows.from(MessageChannels.queue("dedicatedQueueChannel"))
					.bridge(e -> e
							.poller(Pollers.fixedDelay(0).receiveTimeout(-1))
							.taskScheduler(dedicatedTaskScheduler()))
					.channel("dedicatedResults")
					.get();
		}


		@Bean
		public TaskScheduler dedicatedTaskScheduler() {
			return new ThreadPoolTaskScheduler();
		}

		@Bean
		public IntegrationFlow flowWithNullChannel() {
			return IntegrationFlows.from("flowWithNullChannelInput")
					.nullChannel();
		}

		@Bean
		public IntegrationFlow flowWithLocalNullChannel() {
			return f -> f.channel(new NullChannel());
		}

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public AbstractReplyProducingMessageHandler myHandler() {
			return new AbstractReplyProducingMessageHandler() {

				@Override
				protected Object handleRequestMessage(Message<?> requestMessage) {
					return requestMessage;
				}

			};
		}

		@Bean
		public IntegrationFlow flow1WithPrototypeHandler(
				@Qualifier("myHandler") AbstractReplyProducingMessageHandler handler) {
			return f -> f.handle(handler, e -> e.id("flow1WithPrototypeHandlerConsumer"));
		}

		@Bean
		public IntegrationFlow flow2WithPrototypeHandler(
				@Qualifier("myHandler") AbstractReplyProducingMessageHandler handler) {
			return f -> f.handle(handler, e -> e.id("flow2WithPrototypeHandlerConsumer"));
		}

		@Bean
		public IntegrationFlow globalErrorChannelResolutionFlow(@Qualifier("taskScheduler") TaskExecutor taskExecutor) {
			return IntegrationFlows.from(Consumer.class,
					(gateway) -> gateway.beanName("globalErrorChannelResolutionFunction"))
					.channel(c -> c.executor(taskExecutor))
					.handle((p, h) -> {
						throw new RuntimeException("intentional");
					})
					.get();
		}

	}

	@Configuration
	public static class InterceptorContextConfiguration {

		@Bean
		public List<String> outputStringList() {
			return new ArrayList<>();
		}

		@Bean
		public IntegrationFlow interceptorFlow(List<String> outputStringList) {
			return IntegrationFlows.from("interceptorChannelIn")
					.intercept(new ChannelInterceptor() {

						@Override
						public Message<?> preSend(Message<?> message, MessageChannel channel) {
							outputStringList.add("Pre send transform: " + message.getPayload());
							return message;
						}

						@Override
						public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
							outputStringList.add("Post send transform: " + message.getPayload());
						}
					})
					.transform((String s) -> s.toUpperCase())
					.intercept(new ChannelInterceptor() {

						@Override
						public Message<?> preSend(Message<?> message, MessageChannel channel) {
							outputStringList.add("Pre send handle: " + message.getPayload());
							return message;
						}

						@Override
						public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
							outputStringList.add("Post send handle: " + message.getPayload());
						}
					})
					.handle(m -> outputStringList.add("Handle: " + m.getPayload())).get();
		}

	}

	@Service
	public static class GreetingService extends AbstractReplyProducingMessageHandler {

		@Autowired
		private WorldService worldService;

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return "Hello " + this.worldService.world() + " and " + requestMessage.getPayload();
		}

	}

	@Service
	public static class WorldService {

		public String world() {
			return "World";
		}

	}


	private static class InvalidLastMessageChannelFlowContext {

		@Bean
		public IntegrationFlow wrongLastComponent() {
			return IntegrationFlows.from(MessageChannels.direct())
					.fixedSubscriberChannel()
					.get();
		}

	}

	@SuppressWarnings("serial")
	public static class Foo implements Serializable {

		private final Integer value;

		public Foo(Integer value) {
			this.value = value;
		}

	}

}

