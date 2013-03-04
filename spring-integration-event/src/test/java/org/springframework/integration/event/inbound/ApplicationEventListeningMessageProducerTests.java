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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.DirectFieldAccessor;
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
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.GenericMessage;
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
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertNull(message1);
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
		Map retrieverCache = TestUtils.getPropertyValue(multicaster, "retrieverCache", Map.class);

		ctx.publishEvent(new TestApplicationEvent1());

		//Before retrieverCache grew exponentially: the same ApplicationEventListeningMessageProducer for each event
		assertEquals(2, retrieverCache.size());
		for (Object key : retrieverCache.keySet()) {
			Class<? extends ApplicationEvent> event = TestUtils.getPropertyValue(key, "eventType", Class.class);
			assertThat(event, Matchers.is(Matchers.isOneOf(ContextRefreshedEvent.class, TestApplicationEvent1.class)));
			Set listeners = TestUtils.getPropertyValue(retrieverCache.get(key), "applicationListenerBeans", Set.class);
			assertEquals(1, listeners.size());
			assertEquals("testListener", listeners.iterator().next());
		}

		TestApplicationEvent2 event2 = new TestApplicationEvent2();
		ctx.publishEvent(event2);
		assertEquals(3, retrieverCache.size());
		for (Object key : retrieverCache.keySet()) {
			Class event = TestUtils.getPropertyValue(key, "eventType", Class.class);
			if (TestApplicationEvent2.class.isAssignableFrom(event)) {
				Set listeners = TestUtils.getPropertyValue(retrieverCache.get(key), "applicationListenerBeans", Set.class);
				assertEquals(2, listeners.size());
				for (Object listener : listeners) {
					assertThat((String) listener, Matchers.is(Matchers.isOneOf("testListenerMessageProducer", "testListener")));
				}
				break;
			}
		}

		ctx.publishEvent(new ApplicationEvent("Some event") {
		});

		assertEquals(4, listenerCounter.get());

		final Message<?> receive = channel.receive(10);
		assertNotNull(receive);
		assertSame(event2, receive.getPayload());
		assertNull(channel.receive(1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt2935AELMPConcurrency() throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();

		SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
		multicaster = Mockito.spy(multicaster);
		multicaster.setTaskExecutor(executor);

		final AtomicInteger receivedMessagesCounter = new AtomicInteger();

		SubscribableChannel output = new DirectChannel();

		output.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				receivedMessagesCounter.incrementAndGet();
			}
		});

		final ApplicationEventListeningMessageProducer listenerMessageProducer = new ApplicationEventListeningMessageProducer();
		Log logger = TestUtils.getPropertyValue(listenerMessageProducer, "logger", Log.class);
		logger = Mockito.spy(logger);
		listenerMessageProducer.setOutputChannel(output);
		listenerMessageProducer.setEventTypes(TestApplicationEvent1.class);
		DirectFieldAccessor dfa = new DirectFieldAccessor(listenerMessageProducer);
		dfa.setPropertyValue("applicationEventMulticaster", multicaster);
		dfa.setPropertyValue("logger", logger);
		multicaster.addApplicationListener(listenerMessageProducer);

		final Object defaultRetriever = TestUtils.getPropertyValue(multicaster, "defaultRetriever");

		//Emulate delay on re-init of multicaster ApplicationListeners
		Mockito.doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				synchronized (defaultRetriever) {
					try {
						Thread.sleep(10);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return invocation.callRealMethod();
				}
			}
		}).when(multicaster).addApplicationListener(Mockito.any(ApplicationListener.class));

		final AtomicInteger eventsCounter = new AtomicInteger();

		List<Class<? extends ApplicationEvent>> eventTypes = Arrays.asList(TestApplicationEvent2.class, TestApplicationEvent1.class);

		final AtomicInteger eventTypesChangerCounter = new AtomicInteger();

		for (final Class<? extends ApplicationEvent> eventType : eventTypes) {
			executor.execute(new Runnable() {
				int i = eventTypesChangerCounter.getAndIncrement();

				public void run() {
					while (true) {
						if (eventsCounter.get() > i * 3 + 3) {
							break;
						}
					}
					listenerMessageProducer.setEventTypes(eventType);
				}
			});
		}

		listenerMessageProducer.start();

		for (int i = 0; i < 10; i++) {
			multicaster.multicastEvent(new TestApplicationEvent1());
			eventsCounter.incrementAndGet();
			Thread.sleep(10);
		}

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(receivedMessagesCounter.get(), Matchers.allOf(Matchers.greaterThan(3), Matchers.lessThan(10)));

		//ApplicationEventListeningMessageProducer: started
		//Received event: ... was discarded after change of 'eventTypes':
		Mockito.verify(logger, Mockito.atLeast(2)).info(Mockito.anyObject());
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
