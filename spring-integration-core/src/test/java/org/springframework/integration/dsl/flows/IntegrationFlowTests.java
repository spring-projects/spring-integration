/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.dsl.flows;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.transformer.PayloadSerializingTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Tim Ysewyn
 * @author Gary Russell
 * @author Oleg Zhurakousky
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
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

	@Autowired
	@Qualifier("gatewayInput")
	private MessageChannel gatewayInput;

	@Autowired
	@Qualifier("gatewayError")
	private PollableChannel gatewayError;

	@Test
	public void testWithSupplierMessageSourceImpliedPoller() {
		assertEquals("FOO", this.suppliedChannel.receive(1000).getPayload());
	}

	@Test
	public void testWithSupplierMessageSourceProvidedPoller() {
		assertEquals("FOO", this.suppliedChannel2.receive(2000).getPayload());
	}

	@Test
	public void testDirectFlow() {
		assertTrue(this.beanFactory.containsBean("filter"));
		assertTrue(this.beanFactory.containsBean("filter.handler"));
		assertTrue(this.beanFactory.containsBean("expressionFilter"));
		assertTrue(this.beanFactory.containsBean("expressionFilter.handler"));
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("100").setReplyChannel(replyChannel).build();
		try {
			this.inputChannel.send(message);
			fail("Expected MessageDispatchingException");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getCause(), instanceOf(MessageDispatchingException.class));
			assertThat(e.getMessage(), containsString("Dispatcher has no subscribers"));
		}
		this.controlBus.send("@payloadSerializingTransformer.start()");

		final AtomicBoolean used = new AtomicBoolean();

		this.foo.subscribe(m -> used.set(true));

		this.inputChannel.send(message);
		Message<?> reply = replyChannel.receive(5000);
		assertNotNull(reply);
		assertEquals(200, reply.getPayload());

		Message<?> successMessage = this.successChannel.receive(5000);
		assertNotNull(successMessage);
		assertEquals(100, successMessage.getPayload());

		assertTrue(used.get());

		this.inputChannel.send(new GenericMessage<Object>(1000));
		Message<?> discarded = this.discardChannel.receive(5000);
		assertNotNull(discarded);
		assertEquals("Discarded: 1000", discarded.getPayload());
	}

	@Test
	public void testBridge() {
		GenericMessage<String> message = new GenericMessage<>("test");
		this.bridgeFlowInput.send(message);
		Message<?> reply = this.bridgeFlowOutput.receive(5000);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());

		assertTrue(this.beanFactory.containsBean("bridgeFlow2.channel#0"));
		assertThat(this.beanFactory.getBean("bridgeFlow2.channel#0"), instanceOf(FixedSubscriberChannel.class));

		try {
			this.bridgeFlow2Input.send(message);
			fail("Expected MessageDispatchingException");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getCause(), instanceOf(MessageDispatchingException.class));
			assertThat(e.getMessage(), containsString("Dispatcher has no subscribers"));
		}
		this.controlBus.send("@bridge.start()");
		this.bridgeFlow2Input.send(message);
		reply = this.bridgeFlow2Output.receive(5000);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertTrue(this.delayedAdvice.getInvoked());
	}

	@Test
	public void testWrongLastMessageChannel() {
		ConfigurableApplicationContext context = null;
		try {
			context = new AnnotationConfigApplicationContext(InvalidLastMessageChannelFlowContext.class);
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));
			assertThat(e.getMessage(), containsString("'.fixedSubscriberChannel()' " +
					"can't be the last EIP-method in the 'IntegrationFlow' definition"));
		}
		finally {
			if (context != null) {
				context.close();
			}
		}
	}

	@Test
	public void testMethodInvokingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("world")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.methodInvokingInput.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertEquals("Hello World and world", receive.getPayload());
	}

	@Test
	public void testLambdas() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("World")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.lambdasInput.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertEquals("Hello World", receive.getPayload());

		message = MessageBuilder.withPayload("Spring")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();

		this.lambdasInput.send(message);
		assertNull(replyChannel.receive(10));

	}

	@Test
	public void testClaimCheck() {
		QueueChannel replyChannel = new QueueChannel();

		Message<String> message = MutableMessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		this.claimCheckInput.send(message);

		Message<?> receive = replyChannel.receive(2000);
		assertNotNull(receive);
		assertSame(message, receive);

		assertEquals(1, this.messageStore.getMessageCount());
		assertSame(message, this.messageStore.getMessage(message.getHeaders().getId()));
	}

	@Test
	public void testGatewayFlow() throws Exception {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		this.gatewayInput.send(message);

		Message<?> receive = replyChannel.receive(2000);
		assertNotNull(receive);
		assertEquals("From Gateway SubFlow: FOO", receive.getPayload());
		assertNull(this.gatewayError.receive(1));

		message = MessageBuilder.withPayload("bar").setReplyChannel(replyChannel).build();

		this.gatewayInput.send(message);

		receive = replyChannel.receive(1);
		assertNull(receive);

		receive = this.gatewayError.receive(2000);
		assertNotNull(receive);
		assertThat(receive, instanceOf(ErrorMessage.class));
		assertThat(receive.getPayload(), instanceOf(MessageRejectedException.class));
		assertThat(((Exception) receive.getPayload()).getMessage(), containsString("' rejected Message"));
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
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertNull(this.tapChannel.receive(0));

		this.tappedChannel2.send(new GenericMessage<>("foo"));
		this.tappedChannel2.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertNull(this.tapChannel.receive(0));

		this.tappedChannel3.send(new GenericMessage<>("foo"));
		this.tappedChannel3.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertNull(this.tapChannel.receive(0));

		this.tappedChannel4.send(new GenericMessage<>("foo"));
		this.tappedChannel4.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("bar", out.getPayload());

		this.tappedChannel5.send(new GenericMessage<>("foo"));
		out = this.wireTapSubflowResult.receive(10000);
		assertNotNull(out);
		assertEquals("FOO", out.getPayload());
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

		Message<?> receive1 = this.subscriber1Results.receive(5000);
		assertNotNull(receive1);
		assertEquals(1, receive1.getPayload());

		Message<?> receive2 = this.subscriber2Results.receive(5000);
		assertNotNull(receive2);
		assertEquals(4, receive2.getPayload());
		Message<?> receive3 = this.subscriber3Results.receive(5000);
		assertNotNull(receive3);
		assertEquals(6, receive3.getPayload());
	}

	@Autowired
	@Qualifier("errorRecovererFunction")
	private Function<String, String> errorRecovererFlowGateway;

	@Test
	public void testReplyChannelFromReplyMessage() {
		assertEquals("foo", this.errorRecovererFlowGateway.apply("foo"));
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

		assertTrue(resultLatch.await(10, TimeUnit.SECONDS));

		assertEquals("dedicatedTaskScheduler-1", threadNameReference.get());
	}

	@MessagingGateway
	public interface ControlBusGateway {

		void send(String command);
	}

	@Configuration
	@EnableIntegration
	public static class SupplierContextConfiguration1 {
		@Bean
		public IntegrationFlow supplierFlow() {
			return IntegrationFlows.from(() -> "foo")
					.<String, String>transform(p -> p.toUpperCase())
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
			return IntegrationFlows.from(() -> "foo", c -> c.poller(Pollers.fixedDelay(100).maxMessagesPerPoll(1)))
					.<String, String>transform(p -> p.toUpperCase())
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
			return IntegrationFlows.from(ControlBusGateway.class)
					.controlBus()
					.get();
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata poller() {
			return Pollers.fixedRate(500).get();
		}

		@Bean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
		public TaskScheduler taskScheduler() {
			ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			threadPoolTaskScheduler.setPoolSize(100);
			return threadPoolTaskScheduler;
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
			advice.setOnSuccessExpressionString("payload");
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
					.<Integer, Foo>transform(i -> new Foo(i))
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
		public IntegrationFlow wireTapFlow1() {
			return IntegrationFlows.from("tappedChannel1")
					.wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
					.channel("nullChannel")
					.get();
		}

		@Bean
		public IntegrationFlow wireTapFlow2() {
			return f -> f
					.wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
					.channel("nullChannel");
		}

		@Bean
		public IntegrationFlow wireTapFlow3() {
			return f -> f
					.transform("payload")
					.wireTap("tapChannel", wt -> wt.selector("payload == 'foo'"))
					.channel("nullChannel");
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
							.<String, String>transform(String::toUpperCase)
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
			assertEquals(100, payload);
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
					.filter("World"::equals)
					.transform("Hello "::concat)
					.get();
		}

		@Bean
		public IntegrationFlow gatewayFlow() {
			return IntegrationFlows.from("gatewayInput")
					.gateway("gatewayRequest", g -> g.errorChannel("gatewayError").replyTimeout(10L))
					.gateway(f -> f.transform("From Gateway SubFlow: "::concat))
					.get();
		}

		@Bean
		public IntegrationFlow gatewayRequestFlow() {
			return IntegrationFlows.from("gatewayRequest")
					.filter("foo"::equals, f -> f.throwExceptionOnRejection(true))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public MessageChannel gatewayError() {
			return MessageChannels.queue().get();
		}

		@Bean
		public IntegrationFlow errorRecovererFlow() {
			return IntegrationFlows.from(Function.class, "errorRecovererFunction")
					.handle((GenericHandler<?>) (p, h) -> {
						throw new RuntimeException("intentional");
					}, e -> e.advice(retryAdvice()))
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

