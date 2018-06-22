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

package org.springframework.integration.dsl.manualflow;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@ContextConfiguration(classes = ManualFlowTests.RootConfiguration.class)
@RunWith(SpringRunner.class)
@DirtiesContext
public class ManualFlowTests {

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private SmartLifecycleRoleController roleController;

	@Test
	public void testWithAnonymousMessageProducerStart() {
		final AtomicBoolean started = new AtomicBoolean();
		MessageProducerSupport producer = new MessageProducerSupport() {

			@Override
			protected void doStart() {
				started.set(true);
				super.doStart();
			}

		};
		QueueChannel channel = new QueueChannel();
		IntegrationFlow flow = IntegrationFlows.from(producer)
				.channel(channel)
				.get();
		IntegrationFlowRegistration flowRegistration = this.integrationFlowContext.registration(flow).register();
		assertTrue(started.get());

		flowRegistration.destroy();
	}

	@Test
	public void testWithAnonymousMessageProducerSpecStart() {
		final AtomicBoolean started = new AtomicBoolean();
		class MyProducer extends MessageProducerSupport {

			@Override
			protected void doStart() {
				started.set(true);
				super.doStart();
			}

		}
		class MyProducerSpec extends MessageProducerSpec<MyProducerSpec, MyProducer> {

			MyProducerSpec(MyProducer producer) {
				super(producer);
			}

		}
		MyProducerSpec spec = new MyProducerSpec(new MyProducer());
		QueueChannel channel = new QueueChannel();
		IntegrationFlow flow = IntegrationFlows.from(spec.id("fooChannel"))
				.channel(channel)
				.get();
		IntegrationFlowRegistration flowRegistration = this.integrationFlowContext.registration(flow).register();
		assertTrue(started.get());

		flowRegistration.destroy();
	}

	@Test
	public void testManualFlowRegistration() throws InterruptedException {
		String flowId = "testManualFlow";

		IntegrationFlow myFlow = f -> f
				.<String, String>transform(String::toUpperCase)
				.channel(MessageChannels.queue())
				.transform("Hello, "::concat, e -> e
						.poller(p -> p
								.fixedDelay(10)
								.maxMessagesPerPoll(1)
								.receiveTimeout(10)))
				.handle(new BeanFactoryHandler(), e -> e.id("anId"));

		BeanFactoryHandler additionalBean = new BeanFactoryHandler();
		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(myFlow)
						.id(flowId)
						.useFlowIdAsPrefix()
						.addBean(additionalBean)
						.register();

		BeanFactoryHandler bean =
				this.beanFactory.getBean(flowRegistration.getId() + BeanFactoryHandler.class.getName() + "#0",
						BeanFactoryHandler.class);
		assertSame(additionalBean, bean);
		assertSame(this.beanFactory, bean.beanFactory);
		bean = this.beanFactory.getBean(flowRegistration.getId() + "." + "anId.handler", BeanFactoryHandler.class);

		MessagingTemplate messagingTemplate = flowRegistration.getMessagingTemplate();
		messagingTemplate.setReceiveTimeout(10000);

		assertEquals("Hello, FOO", messagingTemplate.convertSendAndReceive("foo", String.class));

		assertEquals("Hello, BAR", messagingTemplate.convertSendAndReceive("bar", String.class));

		try {
			messagingTemplate.receive();
			fail("UnsupportedOperationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(UnsupportedOperationException.class));
			assertThat(e.getMessage(), containsString("The 'receive()/receiveAndConvert()' isn't supported"));
		}

		assertThat(this.beanFactory.getBeanNamesForType(MessageTransformingHandler.class)[0], startsWith(flowId + "."));

		flowRegistration.destroy();

		assertFalse(this.beanFactory.containsBean(flowRegistration.getId()));
		assertFalse(this.beanFactory.containsBean(flowRegistration.getId() + ".input"));
		assertFalse(this.beanFactory.containsBean(flowRegistration.getId() + BeanFactoryHandler.class.getName() + "#0"));

		ThreadPoolTaskScheduler taskScheduler = this.beanFactory.getBean(ThreadPoolTaskScheduler.class);

		int n = 0;
		while (taskScheduler.getActiveCount() > 0 && n++ < 100) {
			Thread.sleep(100);
		}
		assertThat(n, lessThan(100));

		assertTrue(additionalBean.destroyed);
	}

	@Test
	public void testWrongLifecycle() {
		try {
			this.integrationFlowContext.remove("foo");
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(),
					containsString("An IntegrationFlow with the id [" + "foo" + "] doesn't exist in the registry."));
		}
	}

	@Test
	public void testDynamicSubFlow() {
		PollableChannel resultChannel = new QueueChannel();

		this.integrationFlowContext.registration(flow ->
				flow.publishSubscribeChannel(p -> p
						.minSubscribers(1)
						.subscribe(f -> f.channel(resultChannel))
				))
				.id("dynamicFlow")
				.register();

		this.integrationFlowContext.messagingTemplateFor("dynamicFlow").send(new GenericMessage<>("test"));

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test", receive.getPayload());

		MessageHistory messageHistory = MessageHistory.read(receive);
		assertNotNull(messageHistory);
		String messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString, Matchers.containsString("dynamicFlow.input"));
		assertThat(messageHistoryString, Matchers.containsString("dynamicFlow.subFlow#0.channel#1"));

		this.integrationFlowContext.remove("dynamicFlow");
	}

	@Test
	public void testDynamicAdapterFlow() {
		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(new MyFlowAdapter()).register();
		PollableChannel resultChannel = this.beanFactory.getBean("flowAdapterOutput", PollableChannel.class);

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("flowAdapterMessage", receive.getPayload());

		flowRegistration.destroy();
	}


	@Test
	public void testWrongIntegrationFlowScope() {
		try {
			new AnnotationConfigApplicationContext(InvalidIntegrationFlowScopeConfiguration.class).close();
			fail("BeanCreationNotAllowedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationNotAllowedException.class));
			assertThat(e.getMessage(), containsString("IntegrationFlows can not be scoped beans."));
		}
	}

	@Test
	public void testMessageProducerForOutputChannel() {
		class MessageProducingHandler implements MessageHandler, MessageProducer {

			private MessageChannel outputChannel;

			@Override
			public void setOutputChannel(MessageChannel outputChannel) {
				this.outputChannel = outputChannel;
			}

			@Override
			public MessageChannel getOutputChannel() {
				return this.outputChannel;
			}

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				this.outputChannel.send(message);
			}

		}


		PollableChannel resultChannel = new QueueChannel();

		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(
						flow -> flow
								.handle(new MessageProducingHandler())
								.channel(resultChannel))
						.register();

		this.integrationFlowContext.messagingTemplateFor(flowRegistration.getId())
				.send(new GenericMessage<>("test"));

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test", receive.getPayload());

		flowRegistration.destroy();
	}

	@Test
	public void testRoleControl() {
		String testRole = "bridge";

		PollableChannel resultChannel = new QueueChannel();

		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext
						.registration(flow -> flow
								.bridge(e -> e.role(testRole))
								.channel(resultChannel))
						.register();

		MessagingTemplate messagingTemplate =
				this.integrationFlowContext.messagingTemplateFor(flowRegistration.getId());

		messagingTemplate.send(new GenericMessage<>("test"));

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test", receive.getPayload());

		this.roleController.stopLifecyclesInRole(testRole);

		try {
			messagingTemplate.send(new GenericMessage<>("test2"));
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getMessage(), containsString("Dispatcher has no subscribers for channel"));
		}

		this.roleController.startLifecyclesInRole(testRole);

		messagingTemplate.send(new GenericMessage<>("test2"));

		receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test2", receive.getPayload());

		flowRegistration.destroy();

		assertTrue(this.roleController.getEndpointsRunningStatus(testRole).isEmpty());
	}

	@Test
	public void testDynamicSubFlowCreation() {
		Flux<Message<?>> messageFlux =
				Flux.just("1,2,3,4")
						.map(v -> v.split(","))
						.flatMapIterable(Arrays::asList)
						.map(Integer::parseInt)
						.map(GenericMessage::new);

		QueueChannel resultChannel = new QueueChannel();

		IntegrationFlow integrationFlow = IntegrationFlows
				.from(messageFlux)
				.<Integer, Boolean>route(p -> p % 2 == 0, m -> m
						.subFlowMapping(true, sf -> sf.<Integer, String>transform(em -> "even:" + em))
						.subFlowMapping(false, sf -> sf.<Integer, String>transform(em -> "odd:" + em))
						.defaultOutputToParentFlow()
				)
				.channel(resultChannel)
				.get();

		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(integrationFlow)
						.id("dynamicSubFlows")
						.register();

		for (int i = 0; i < 4; i++) {
			Message<?> receive = resultChannel.receive(10_000);
			assertNotNull(receive);
		}

		assertNull(resultChannel.receive(0));

		flowRegistration.destroy();
	}

	@Test
	public void testRegistrationDuplicationRejected() {
		String testId = "testId";

		StandardIntegrationFlow testFlow =
				IntegrationFlows.from(Supplier.class)
						.get();

		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext
						.registration(testFlow)
						.id(testId)
						.register();

		try {
			this.integrationFlowContext
					.registration(testFlow)
					.id(testId)
					.register();
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalArgumentException.class));
			assertThat(e.getMessage(), containsString("with flowId '" + testId + "' is already registered."));
		}

		flowRegistration.destroy();
	}

	@Test
	public void testConcurrentRegistration() throws InterruptedException {
		ExecutorService executorService = Executors.newCachedThreadPool();

		List<IntegrationFlowRegistration> flowRegistrations = new CopyOnWriteArrayList<>();

		AtomicBoolean exceptionHappened = new AtomicBoolean();

		for (int i = 0; i < 100; i++) {
			int index = i;
			executorService.execute(() -> {

				IntegrationFlow flow = f -> f
						.transform(m -> m);

				try {
					IntegrationFlowContext.IntegrationFlowRegistrationBuilder registration =
							this.integrationFlowContext.registration(flow);
					if (index % 2 == 0) {
						registration.id("concurrentFlow#" + index);
					}
					flowRegistrations.add(registration.register());
				}
				catch (Exception e) {
					exceptionHappened.set(true);
				}

			});
		}

		executorService.shutdownNow();
		assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));

		assertFalse(exceptionHappened.get());

		flowRegistrations.forEach(IntegrationFlowRegistration::destroy);
	}

	@Configuration
	@EnableIntegration
	@EnableMessageHistory
	public static class RootConfiguration {

		@Bean
		public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
			return beanFactory -> ((DefaultListableBeanFactory) beanFactory).setAllowBeanDefinitionOverriding(false);
		}

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public Date foo() {
			return new Date();
		}

	}

	private static class MyFlowAdapter extends IntegrationFlowAdapter {

		private final AtomicReference<Date> nextExecutionTime = new AtomicReference<>(new Date());

		@Override
		protected IntegrationFlowDefinition<?> buildFlow() {
			return from(() -> new GenericMessage<>("flowAdapterMessage"),
					e -> e.poller(p -> p
							.trigger(ctx -> this.nextExecutionTime.getAndSet(null))))
					.channel(MessageChannels.queue("flowAdapterOutput"));

		}

	}

	@Configuration
	@EnableIntegration
	public static class InvalidIntegrationFlowScopeConfiguration {

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public IntegrationFlow wrongScopeFlow() {
			return IntegrationFlowDefinition::bridge;
		}

	}

	private final class BeanFactoryHandler extends AbstractReplyProducingMessageHandler
			implements DisposableBean {

		@Autowired
		private BeanFactory beanFactory;

		private boolean destroyed;

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Objects.requireNonNull(this.beanFactory);
			return requestMessage;
		}

		@Override
		protected void doInit() {
			this.beanFactory.getClass(); // ensure wiring before afterPropertiesSet()
		}

		@Override
		public void destroy() throws Exception {
			this.destroyed = true;
		}

	}

}
