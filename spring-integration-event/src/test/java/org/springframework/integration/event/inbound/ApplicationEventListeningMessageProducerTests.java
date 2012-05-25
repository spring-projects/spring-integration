/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
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
		adapter.onApplicationEvent(new TestApplicationEvent1());
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
		adapter.setEventTypes(new Class[]{TestApplicationEvent1.class});
		adapter.start();
		Message<?> message1 = channel.receive(0);
		assertNull(message1);
		adapter.onApplicationEvent(new TestApplicationEvent1());
		adapter.onApplicationEvent(new TestApplicationEvent2());
		Message<?> message2 = channel.receive(20);
		assertNotNull(message2);
		assertEquals("event1", ((ApplicationEvent) message2.getPayload()).getSource());
		Message<?> message3 = channel.receive(0);
		assertNull(message3);
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

	@Test(expected=MessageHandlingException.class)
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

}
