/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.event.inbound;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class ApplicationEventListeningMessageProducerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Test
	public void anyApplicationEventSentByDefault() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertThat(message1).isNull();
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent1.class))).isTrue();
		adapter.onApplicationEvent(new TestApplicationEvent1());
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent2.class))).isTrue();
		adapter.onApplicationEvent(new TestApplicationEvent2());
		Message<?> message2 = channel.receive(20);
		assertThat(message2).isNotNull();
		assertThat(((ApplicationEvent) message2.getPayload()).getSource()).isEqualTo("event1");
		Message<?> message3 = channel.receive(20);
		assertThat(message3).isNotNull();
		assertThat(((ApplicationEvent) message3.getPayload()).getSource()).isEqualTo("event2");
		adapter.stop();
	}

	@Test
	public void onlyConfiguredEventTypesAreSent() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.setEventTypes(TestApplicationEvent1.class);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertThat(message1).isNull();
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent1.class))).isTrue();
		adapter.onApplicationEvent(new TestApplicationEvent1());
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent2.class))).isFalse();
		Message<?> message2 = channel.receive(20);
		assertThat(message2).isNotNull();
		assertThat(((ApplicationEvent) message2.getPayload()).getSource()).isEqualTo("event1");
		assertThat(channel.receive(0)).isNull();

		adapter.setEventTypes((Class<? extends ApplicationEvent>) null);
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent1.class))).isTrue();
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent2.class))).isTrue();

		adapter.setEventTypes(null, TestApplicationEvent2.class, null);
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent1.class))).isFalse();
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent2.class))).isTrue();

		adapter.setEventTypes(null, null);
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent1.class))).isTrue();
		assertThat(adapter.supportsEventType(ResolvableType.forClass(TestApplicationEvent2.class))).isTrue();
		adapter.stop();
	}

	@Test
	public void applicationContextEvents() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"applicationEventInboundChannelAdapterTests.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("channel");
		Message<?> contextRefreshedEventMessage = channel.receive(0);
		assertThat(contextRefreshedEventMessage).isNotNull();
		assertThat(contextRefreshedEventMessage.getPayload().getClass()).isEqualTo(ContextRefreshedEvent.class);
		context.start();
		Message<?> startedEventMessage = channel.receive(0);
		assertThat(startedEventMessage).isNotNull();
		assertThat(startedEventMessage.getPayload().getClass()).isEqualTo(ContextStartedEvent.class);
		context.stop();
		Message<?> contextStoppedEventMessage = channel.receive(0);
		assertThat(contextStoppedEventMessage).isNotNull();
		assertThat(contextStoppedEventMessage.getPayload().getClass()).isEqualTo(ContextStoppedEvent.class);
		context.close();
		Message<?> closedEventMessage = channel.receive(0);
		assertThat(closedEventMessage).isNotNull();
		assertThat(closedEventMessage.getPayload().getClass()).isEqualTo(ContextClosedEvent.class);
	}

	@Test
	public void payloadExpressionEvaluatedAgainstApplicationEvent() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setPayloadExpression(PARSER.parseExpression("'received: ' + source"));
		adapter.setOutputChannel(channel);

		GenericApplicationContext ctx = TestUtils.createTestApplicationContext();
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
		populateBeanFactory(beanFactory);
		adapter.setBeanFactory(beanFactory);
		beanFactory.registerSingleton("testListenerMessageProducer", adapter);
		adapter.afterPropertiesSet();

		ctx.refresh();

		Message<?> message1 = channel.receive(0);
		// ContextRefreshedEvent
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload().toString()
				.contains("org.springframework.integration.test.util.TestUtils$TestApplicationContext")).isTrue();

		adapter.onApplicationEvent(new TestApplicationEvent1());
		adapter.onApplicationEvent(new TestApplicationEvent2());
		Message<?> message2 = channel.receive(20);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("received: event1");
		Message<?> message3 = channel.receive(20);
		assertThat(message3).isNotNull();
		assertThat(message3.getPayload()).isEqualTo("received: event2");

		ctx.close();
	}

	@Test
	public void messagingEventReceived() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertThat(message1).isNull();
		adapter.onApplicationEvent(new MessagingEvent(new GenericMessage<>("test")));
		Message<?> message2 = channel.receive(20);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("test");
		adapter.stop();
	}

	@Test
	public void messageAsSourceOrCustomEventType() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertThat(message1).isNull();
		adapter.onApplicationEvent(new TestMessagingEvent(new GenericMessage<>("test")));
		Message<?> message2 = channel.receive(20);
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("test");
		adapter.stop();
	}

	@Test
	public void anyApplicationEventCausesExceptionWithErrorHandling() {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				throw new RuntimeException("Failed");
			}
		});
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.start();
		adapter.onApplicationEvent(new TestApplicationEvent1());
		Message<?> message = errorChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(((Exception) message.getPayload()).getCause().getMessage()).isEqualTo("Failed");
		adapter.setErrorChannel(null);
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> adapter.onApplicationEvent(new TestApplicationEvent1()));
		adapter.stop();
	}

	@Test
	public void testInt2935CheckRetrieverCache() {
		GenericApplicationContext ctx = TestUtils.createTestApplicationContext();
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();

		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer listenerMessageProducer =
				new ApplicationEventListeningMessageProducer();
		listenerMessageProducer.setOutputChannel(channel);
		listenerMessageProducer.setEventTypes(TestApplicationEvent2.class);
		beanFactory.registerSingleton("testListenerMessageProducer", listenerMessageProducer);

		AtomicInteger listenerCounter = new AtomicInteger();
		beanFactory.registerSingleton("testListener", new TestApplicationListener(listenerCounter));

		ctx.refresh();

		ApplicationEventMulticaster multicaster =
				ctx.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
						ApplicationEventMulticaster.class);
		Map<?, ?> retrieverCache = TestUtils.getPropertyValue(multicaster, "retrieverCache");

		ctx.publishEvent(new TestApplicationEvent1());

		/*
		 *  Previously, the retrieverCache grew unnecessarily; the adapter was added to the cache for each event type,
		 *  event if not supported.
		 */
		assertThat(retrieverCache.size()).isEqualTo(2);
		for (Map.Entry<?, ?> entry : retrieverCache.entrySet()) {
			Class<? extends ApplicationEvent> event = TestUtils.getPropertyValue(entry.getKey(), "eventType.resolved");
			assertThat(event).isIn(ContextRefreshedEvent.class, TestApplicationEvent1.class);
			Set<?> listeners = TestUtils.getPropertyValue(entry.getValue(), "applicationListeners");
			assertThat(listeners.size()).isEqualTo(1);
			assertThat(listeners.iterator().next()).isSameAs(ctx.getBean("testListener"));
		}

		TestApplicationEvent2 event2 = new TestApplicationEvent2();
		ctx.publishEvent(event2);
		assertThat(retrieverCache.size()).isEqualTo(3);
		for (Map.Entry<?, ?> entry : retrieverCache.entrySet()) {
			Class<?> event = TestUtils.getPropertyValue(entry.getKey(), "eventType.resolved");
			if (TestApplicationEvent2.class.isAssignableFrom(event)) {
				Set<?> listeners = TestUtils.getPropertyValue(entry.getValue(), "applicationListeners");
				assertThat(listeners.size()).isEqualTo(2);
				for (Object listener : listeners) {
					assertThat(listener)
							.isIn(ctx.getBean("testListenerMessageProducer"), ctx.getBean("testListener"));
				}
				break;
			}
		}

		ctx.publishEvent(new ApplicationEvent("Some event") {

		});

		assertThat(listenerCounter.get()).isEqualTo(4);

		final Message<?> receive = channel.receive(10);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isSameAs(event2);
		assertThat(channel.receive(1)).isNull();
		ctx.close();
	}

	private static void populateBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.registerSingleton(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				new SimpleApplicationEventMulticaster(beanFactory));
		beanFactory.registerSingleton(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new StandardEvaluationContext());
	}

	@Test
	public void testPayloadEvents() {
		GenericApplicationContext ctx = TestUtils.createTestApplicationContext();
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();

		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer listenerMessageProducer =
				new ApplicationEventListeningMessageProducer();
		listenerMessageProducer.setOutputChannel(channel);
		listenerMessageProducer.setEventTypes(String.class);
		beanFactory.registerSingleton("testListenerMessageProducer", listenerMessageProducer);

		ctx.refresh();

		ctx.publishEvent("foo");

		Message<?> receive = channel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("foo");

		ctx.close();
	}

	@SuppressWarnings("serial")
	private static class TestApplicationEvent1 extends ApplicationEvent {

		TestApplicationEvent1() {
			super("event1");
		}

	}

	@SuppressWarnings("serial")
	private static class TestApplicationEvent2 extends ApplicationEvent {

		TestApplicationEvent2() {
			super("event2");
		}

	}

	@SuppressWarnings("serial")
	private static class TestMessagingEvent extends ApplicationEvent {

		TestMessagingEvent(Message<?> message) {
			super(message);
		}

	}

	private record TestApplicationListener(AtomicInteger counter) implements ApplicationListener<ApplicationEvent> {

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			this.counter.incrementAndGet();
		}

	}

	static final class ContextEventsChannel implements PollableChannel {

		private final BlockingQueue<Message<?>> internalQueue = new LinkedBlockingQueue<>();

		@Override
		public Message<?> receive() {
			return receive(-1);
		}

		@Override
		public Message<?> receive(long timeout) {
			try {
				return this.internalQueue.poll(timeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			try {
				return this.internalQueue.offer(message, timeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
