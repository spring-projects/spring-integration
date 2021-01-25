/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl.manualflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.util.NoBeansOverrideAnnotationConfigContextLoader;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import reactor.core.publisher.Flux;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@ContextConfiguration(loader = NoBeansOverrideAnnotationConfigContextLoader.class,
		classes = ManualFlowTests.RootConfiguration.class)
@SpringJUnitConfig
@DirtiesContext
public class ManualFlowTests {

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private SmartLifecycleRoleController roleController;

	@Autowired
	private IntegrationManagementConfigurer integrationManagementConfigurer;

	@Test
	@SuppressWarnings("unchecked")
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
		channel.setBeanName("channel");
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(producer);

		BridgeHandler bridgeHandler = new BridgeHandler();
		bridgeHandler.setBeanName("bridge");

		IntegrationFlow flow =
				flowBuilder.handle(bridgeHandler)
						.channel(channel)
						.get();
		IntegrationFlowRegistration flowRegistration = this.integrationFlowContext.registration(flow).register();
		assertThat(started.get()).isTrue();


		Set<MessageProducer> replyProducers =
				TestUtils.getPropertyValue(flowBuilder, "REFERENCED_REPLY_PRODUCERS", Set.class);

		assertThat(replyProducers.contains(bridgeHandler)).isTrue();

		flowRegistration.destroy();

		assertThat(replyProducers.contains(bridgeHandler)).isFalse();
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
		assertThat(started.get()).isTrue();

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

		String flowRegistrationId = flowRegistration.getId();
		BeanFactoryHandler bean =
				this.beanFactory.getBean(flowRegistrationId + BeanFactoryHandler.class.getName() + "#0",
						BeanFactoryHandler.class);
		assertThat(bean).isSameAs(additionalBean);
		assertThat(bean.beanFactory).isSameAs(this.beanFactory);
		bean = this.beanFactory.getBean(flowRegistrationId + "." + "anId.handler", BeanFactoryHandler.class);

		MessagingTemplate messagingTemplate = flowRegistration.getMessagingTemplate();
		messagingTemplate.setReceiveTimeout(10000);

		assertThat(messagingTemplate.convertSendAndReceive("foo", String.class)).isEqualTo("Hello, FOO");

		assertThat(messagingTemplate.convertSendAndReceive("bar", String.class)).isEqualTo("Hello, BAR");

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(messagingTemplate::receive)
				.withMessageContaining("The 'receive()/receiveAndConvert()' isn't supported");

		assertThat(this.beanFactory.getBeanNamesForType(MessageTransformingHandler.class)[0]).startsWith(flowId + ".");

		flowRegistration.destroy();

		assertThat(this.beanFactory.containsBean(flowRegistrationId)).isFalse();
		assertThat(this.beanFactory.containsBean(flowRegistrationId + ".input")).isFalse();
		assertThat(this.beanFactory.containsBean(flowRegistrationId + BeanFactoryHandler.class.getName() + "#0"))
				.isFalse();

		ThreadPoolTaskScheduler taskScheduler = this.beanFactory.getBean(ThreadPoolTaskScheduler.class);

		int n = 0;
		while (taskScheduler.getActiveCount() > 0 && n++ < 100) {
			Thread.sleep(100);
		}
		assertThat(n).isLessThan(100);

		assertThat(additionalBean.destroyed).isTrue();
	}

	@Test
	public void testWrongLifecycle() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.integrationFlowContext.remove("foo"))
				.withMessageContaining("An IntegrationFlow with the id [" + "foo" + "] doesn't exist in the registry.");
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
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("test");

		MessageHistory messageHistory = MessageHistory.read(receive);
		assertThat(messageHistory).isNotNull();
		String messageHistoryString = messageHistory.toString();
		assertThat(messageHistoryString).contains("dynamicFlow.input");
		assertThat(messageHistoryString).contains("dynamicFlow.subFlow#0.channel#1");

		this.integrationFlowContext.remove("dynamicFlow");
	}

	@Test
	public void testDynamicAdapterFlow() {
		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(new MyFlowAdapter()).register();
		PollableChannel resultChannel = this.beanFactory.getBean("flowAdapterOutput", PollableChannel.class);

		Message<?> receive = resultChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("flowAdapterMessage");

		flowRegistration.destroy();
	}


	@Test
	public void testWrongIntegrationFlowScope() {
		assertThatExceptionOfType(BeanCreationNotAllowedException.class)
				.isThrownBy(() ->
						new AnnotationConfigApplicationContext(InvalidIntegrationFlowScopeConfiguration.class))
				.withMessageContaining("IntegrationFlows can not be scoped beans.");
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
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("test");

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
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("test");

		this.roleController.stopLifecyclesInRole(testRole);

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> messagingTemplate.send(new GenericMessage<>("test2")))
				.withMessageContaining("Dispatcher has no subscribers for channel");

		this.roleController.startLifecyclesInRole(testRole);

		messagingTemplate.send(new GenericMessage<>("test2"));

		receive = resultChannel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("test2");

		flowRegistration.destroy();

		assertThat(this.roleController.getEndpointsRunningStatus(testRole).isEmpty()).isTrue();
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
			assertThat(receive).isNotNull();
		}

		assertThat(resultChannel.receive(0)).isNull();

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

		assertThatIllegalArgumentException()
				.isThrownBy(() ->
						this.integrationFlowContext
								.registration(testFlow)
								.id(testId)
								.register())
				.withMessageContaining("with flowId '" + testId + "' is already registered.");

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
		assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		assertThat(exceptionHappened.get()).isFalse();

		flowRegistrations.forEach(IntegrationFlowRegistration::destroy);
	}


	@Test
	public void testDisabledBeansOverride() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						this.integrationFlowContext.registration(f -> f.channel(c -> c.direct("doNotOverrideChannel")))
								.register())
				.isExactlyInstanceOf(BeanCreationException.class)
				.withCauseExactlyInstanceOf(BeanDefinitionOverrideException.class)
				.withMessageContaining("Invalid bean definition with name 'doNotOverrideChannel'");
	}

	@Test
	public void testBeanDefinitionInfoInTheException() {
		IntegrationFlow testFlow = f -> f.<String, String>transform(String::toUpperCase);
		Method source = ReflectionUtils.findMethod(ManualFlowTests.class, "testBeanDefinitionInfoInTheException");
		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(testFlow)
						.setSource(source)
						.register();
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> flowRegistration.getInputChannel().send(new GenericMessage<>(new Date())))
				.withCauseExactlyInstanceOf(ClassCastException.class)
				.withMessageContaining("from source: '" + source + "'")
				.withStackTraceContaining("java.util.Date cannot be cast to");

		flowRegistration.destroy();
	}

	@Configuration
	@EnableIntegration
	@EnableMessageHistory
	@EnableIntegrationManagement
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


		@Bean
		public MessageChannel doNotOverrideChannel() {
			return new DirectChannel();
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

	private final class BeanFactoryHandler extends AbstractReplyProducingMessageHandler {

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
		public void destroy() {
			this.destroyed = true;
		}

	}

}
