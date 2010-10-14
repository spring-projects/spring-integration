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

package org.springframework.integration.event;

import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Mark Fisher
 */
public class ApplicationEventInboundChannelAdapterTests {

	@Test
	public void anyApplicationEventSentByDefault() {
		QueueChannel channel = new QueueChannel();
		ApplicationEventInboundChannelAdapter adapter = new ApplicationEventInboundChannelAdapter();
		adapter.setOutputChannel(channel);
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
		ApplicationEventInboundChannelAdapter adapter = new ApplicationEventInboundChannelAdapter();
		adapter.setOutputChannel(channel);
		adapter.setEventTypes(new Class[]{TestApplicationEvent1.class});
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
		ApplicationEventInboundChannelAdapter adapter = new ApplicationEventInboundChannelAdapter();
		adapter.setPayloadExpression("'received: ' + source");
		adapter.setOutputChannel(channel);
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

}
