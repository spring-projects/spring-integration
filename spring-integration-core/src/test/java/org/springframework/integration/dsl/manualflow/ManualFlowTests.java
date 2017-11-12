/*
 * Copyright 2016-2017 the original author or authors.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowRegistration;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.SmartLifecycleRoleController;
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
	private BeanFactory beanFactory;

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
		this.integrationFlowContext.registration(flow).register();
		assertTrue(started.get());
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
		IntegrationFlow flow = IntegrationFlows.from(spec.id("foo"))
				.channel(channel)
				.get();
		this.integrationFlowContext.registration(flow).register();
		assertTrue(started.get());
	}

	@Test
	public void testManualFlowRegistration() throws InterruptedException {
		IntegrationFlow myFlow = f -> f
				.<String, String>transform(String::toUpperCase)
				.channel(MessageChannels.queue())
				.transform("Hello, "::concat, e -> e
						.poller(p -> p
								.fixedDelay(10)
								.maxMessagesPerPoll(1)
								.receiveTimeout(10)))
				.handle(new BeanFactoryHandler());

		BeanFactoryHandler additionalBean = new BeanFactoryHandler();
		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(myFlow)
						.addBean(additionalBean)
						.register();

		BeanFactoryHandler bean =
				this.beanFactory.getBean(flowRegistration.getId() + BeanFactoryHandler.class.getName() + "#0",
						BeanFactoryHandler.class);
		assertSame(additionalBean, bean);
		assertSame(this.beanFactory, bean.beanFactory);

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

		flowRegistration.destroy();

		assertFalse(this.beanFactory.containsBean(flowRegistration.getId()));
		assertFalse(this.beanFactory.containsBean(flowRegistration.getId() + ".input"));
		assertFalse(this.beanFactory.containsBean(flowRegistration.getId() + BeanFactoryHandler.class.getName() + "#0"));

		ThreadPoolTaskScheduler taskScheduler = this.beanFactory.getBean(ThreadPoolTaskScheduler.class);
		Thread.sleep(100);
		assertEquals(0, taskScheduler.getActiveCount());

		assertTrue(additionalBean.destroyed);
	}

	@Test
	public void testWrongLifecycle() {

		class MyIntegrationFlow implements IntegrationFlow {

			@Override
			public void configure(IntegrationFlowDefinition<?> flow) {
				flow.bridge();
			}

		}

		IntegrationFlow testFlow = new MyIntegrationFlow();

		// This is fine because we are not going to start it automatically.
		assertNotNull(this.integrationFlowContext.registration(testFlow)
				.autoStartup(false)
				.register());

		try {
			this.integrationFlowContext.registration(testFlow).register();
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("Consider to implement it for [" + testFlow + "]."));
		}

		try {
			this.integrationFlowContext.remove("foo");
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("But [" + "foo" + "] ins't one of them."));
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
	}

	@Test
	public void testDynamicAdapterFlow() {
		this.integrationFlowContext.registration(new MyFlowAdapter()).register();
		PollableChannel resultChannel = this.beanFactory.getBean("flowAdapterOutput", PollableChannel.class);

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("flowAdapterMessage", receive.getPayload());
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
				this.integrationFlowContext.registration(flow ->
						flow.handle(new MessageProducingHandler())
								.channel(resultChannel))
						.register();

		this.integrationFlowContext.messagingTemplateFor(flowRegistration.getId())
				.send(new GenericMessage<>("test"));

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test", receive.getPayload());
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

	@Configuration
	@EnableIntegration
	public static class RootConfiguration {

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
