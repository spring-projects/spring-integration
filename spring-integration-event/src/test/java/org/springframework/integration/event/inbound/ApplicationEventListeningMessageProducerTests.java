/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.event.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.Test;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ApplicationEventListeningMessageProducerTests {

	@Test
	public void anyApplicationEventSentByDefault() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertNull(message1);
		assertTrue(adapter.supportsEventType(TestApplicationEvent1.class));
		adapter.onApplicationEvent(new TestApplicationEvent1());
		assertTrue(adapter.supportsEventType(TestApplicationEvent2.class));
		adapter.onApplicationEvent(new TestApplicationEvent2());
		Message<?> message2 = channel.receive(20);
		assertNotNull(message2);
		assertEquals("event1", ((ApplicationEvent) message2.getPayload()).getSource());
		Message<?> message3 = channel.receive(20);
		assertNotNull(message3);
		assertEquals("event2", ((ApplicationEvent) message3.getPayload()).getSource());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void onlyConfiguredEventTypesAreSent() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.setEventTypes(TestApplicationEvent1.class);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertNull(message1);
		assertTrue(adapter.supportsEventType(TestApplicationEvent1.class));
		adapter.onApplicationEvent(new TestApplicationEvent1());
		assertFalse(adapter.supportsEventType(TestApplicationEvent2.class));
		Message<?> message2 = channel.receive(20);
		assertNotNull(message2);
		assertEquals("event1", ((ApplicationEvent) message2.getPayload()).getSource());
		assertNull(channel.receive(0));

		adapter.setEventTypes((Class<? extends ApplicationEvent>) null);
		assertTrue(adapter.supportsEventType(TestApplicationEvent1.class));
		assertTrue(adapter.supportsEventType(TestApplicationEvent2.class));

		adapter.setEventTypes(null, TestApplicationEvent2.class, null);
		assertFalse(adapter.supportsEventType(TestApplicationEvent1.class));
		assertTrue(adapter.supportsEventType(TestApplicationEvent2.class));

		adapter.setEventTypes(null, null);
		assertTrue(adapter.supportsEventType(TestApplicationEvent1.class));
		assertTrue(adapter.supportsEventType(TestApplicationEvent2.class));
	}

	@Test
	public void applicationContextEvents() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"applicationEventInboundChannelAdapterTests.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("channel");
		Message<?> contextRefreshedEventMessage = channel.receive(0);
		assertNotNull(contextRefreshedEventMessage);
		assertEquals(ContextRefreshedEvent.class, contextRefreshedEventMessage.getPayload().getClass());
		context.start();
		Message<?> startedEventMessage = channel.receive(0);
		assertNotNull(startedEventMessage);
		assertEquals(ContextStartedEvent.class, startedEventMessage.getPayload().getClass());
		context.stop();
		Message<?> contextStoppedEventMessage = channel.receive(0);
		assertNotNull(contextStoppedEventMessage);
		assertEquals(ContextStoppedEvent.class, contextStoppedEventMessage.getPayload().getClass());
		context.close();
		Message<?> closedEventMessage = channel.receive(0);
		assertNotNull(closedEventMessage);
		assertEquals(ContextClosedEvent.class, closedEventMessage.getPayload().getClass());
	}

	@Test
	public void payloadExpressionEvaluatedAgainstApplicationEvent() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setPayloadExpression("'received: ' + source");
		adapter.setOutputChannel(channel);

		GenericApplicationContext ctx = TestUtils.createTestApplicationContext();
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
		beanFactory.registerSingleton(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				new SimpleApplicationEventMulticaster(beanFactory));

		adapter.setBeanFactory(beanFactory);
		beanFactory.registerSingleton("testListenerMessageProducer", adapter);
		adapter.afterPropertiesSet();

		ctx.refresh();

		Message<?> message1 = channel.receive(0);
		// ContextRefreshedEvent
		assertNotNull(message1);
		assertTrue(message1.getPayload().toString()
				.contains("org.springframework.integration.test.util.TestUtils$TestApplicationContext"));

		adapter.onApplicationEvent(new TestApplicationEvent1());
		adapter.onApplicationEvent(new TestApplicationEvent2());
		Message<?> message2 = channel.receive(20);
		assertNotNull(message2);
		assertEquals("received: event1", message2.getPayload());
		Message<?> message3 = channel.receive(20);
		assertNotNull(message3);
		assertEquals("received: event2", message3.getPayload());
	}

	@Test
	public void messagingEventReceived() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertNull(message1);
		adapter.onApplicationEvent(new MessagingEvent(new GenericMessage<String>("test")));
		Message<?> message2 = channel.receive(20);
		assertNotNull(message2);
		assertEquals("test", message2.getPayload());
	}

	@Test
	public void messageAsSourceOrCustomEventType() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer adapter = new ApplicationEventListeningMessageProducer();
		adapter.setOutputChannel(channel);
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertNull(message1);
		adapter.onApplicationEvent(new TestMessagingEvent(new GenericMessage<String>("test")));
		Message<?> message2 = channel.receive(20);
		assertNotNull(message2);
		assertEquals("test", message2.getPayload());
	}

	@Test(expected = MessageHandlingException.class)
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
		assertNotNull(message);
		assertEquals("Failed", ((Exception) message.getPayload()).getCause().getMessage());
		adapter.setErrorChannel(null);
		adapter.onApplicationEvent(new TestApplicationEvent1());
	}

	@Test
	@SuppressWarnings({"unchecked", "serial"})
	public void testInt2935CheckRetrieverCache() {
		GenericApplicationContext ctx = TestUtils.createTestApplicationContext();
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();

		QueueChannel channel = new QueueChannel();
		ApplicationEventListeningMessageProducer listenerMessageProducer = new ApplicationEventListeningMessageProducer();
		listenerMessageProducer.setOutputChannel(channel);
		listenerMessageProducer.setEventTypes(TestApplicationEvent2.class);
		beanFactory.registerSingleton("testListenerMessageProducer", listenerMessageProducer);

		AtomicInteger listenerCounter = new AtomicInteger();
		beanFactory.registerSingleton("testListener", new TestApplicationListener(listenerCounter));

		ctx.refresh();

		ApplicationEventMulticaster multicaster =
				ctx.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
		Map<?, ?> retrieverCache = TestUtils.getPropertyValue(multicaster, "retrieverCache", Map.class);

		ctx.publishEvent(new TestApplicationEvent1());

		/*
		 *  Previously, the retrieverCache grew unnecessarily; the adapter was added to the cache for each event type,
		 *  event if not supported.
		 */
		assertEquals(2, retrieverCache.size());
		for (Object key : retrieverCache.keySet()) {
			Class<? extends ApplicationEvent> event = TestUtils.getPropertyValue(key, "eventType", Class.class);
			assertThat(event, Matchers.is(Matchers.isOneOf(ContextRefreshedEvent.class, TestApplicationEvent1.class)));
			Set<?> listeners = TestUtils.getPropertyValue(retrieverCache.get(key), "applicationListenerBeans", Set.class);
			assertEquals(1, listeners.size());
			assertEquals("testListener", listeners.iterator().next());
		}

		TestApplicationEvent2 event2 = new TestApplicationEvent2();
		ctx.publishEvent(event2);
		assertEquals(3, retrieverCache.size());
		for (Object key : retrieverCache.keySet()) {
			Class<?> event = TestUtils.getPropertyValue(key, "eventType", Class.class);
			if (TestApplicationEvent2.class.isAssignableFrom(event)) {
				Set<?> listeners = TestUtils.getPropertyValue(retrieverCache.get(key), "applicationListenerBeans", Set.class);
				assertEquals(2, listeners.size());
				for (Object listener : listeners) {
					assertThat((String) listener, Matchers.is(Matchers.isOneOf("testListenerMessageProducer", "testListener")));
				}
				break;
			}
		}

		ctx.publishEvent(new ApplicationEvent("Some event") {});

		assertEquals(4, listenerCounter.get());

		final Message<?> receive = channel.receive(10);
		assertNotNull(receive);
		assertSame(event2, receive.getPayload());
		assertNull(channel.receive(1));
	}


	@SuppressWarnings("serial")
	private static class TestApplicationEvent1 extends ApplicationEvent {

		public TestApplicationEvent1() {
			super("event1");
		}
	}

	@SuppressWarnings("serial")
	private static class TestApplicationEvent2 extends ApplicationEvent {

		public TestApplicationEvent2() {
			super("event2");
		}
	}

	@SuppressWarnings("serial")
	private static class TestMessagingEvent extends ApplicationEvent {

		public TestMessagingEvent(Message<?> message) {
			super(message);
		}
	}

	private static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

		private final AtomicInteger counter;

		private TestApplicationListener(AtomicInteger counter) {
			this.counter = counter;
		}

		public void onApplicationEvent(ApplicationEvent event) {
			this.counter.incrementAndGet();
		}
	}

}
