/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class ApplicationEventSourceTests {

	@Test
	public void testAnyApplicationEventSentByDefault() {
		MessageChannel channel = new QueueChannel();
		ApplicationEventSource adapter = new ApplicationEventSource(channel);
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
	public void testOnlyConfiguredEventTypesAreSent() {
		MessageChannel channel = new QueueChannel();
		ApplicationEventSource adapter = new ApplicationEventSource(channel);
		List<Class<? extends ApplicationEvent>> eventTypes = new ArrayList<Class<? extends ApplicationEvent>>();
		eventTypes.add(TestApplicationEvent1.class);
		adapter.setEventTypes(eventTypes);
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
	public void testApplicationContextEvents() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationEventSourceTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> refreshedEventMessage = channel.receive(0);
		assertNotNull(refreshedEventMessage);
		assertEquals(ContextRefreshedEvent.class, refreshedEventMessage.getPayload().getClass());
		context.start();
		Message<?> startedEventMessage = channel.receive(0);
		assertNotNull(startedEventMessage);
		assertEquals(ContextStartedEvent.class, startedEventMessage.getPayload().getClass());
		context.stop();
		Message<?> stoppedEventMessage = channel.receive(0);
		assertNotNull(stoppedEventMessage);
		assertEquals(ContextStoppedEvent.class, stoppedEventMessage.getPayload().getClass());		
		context.close();
		Message<?> closedEventMessage = channel.receive(0);
		assertNotNull(closedEventMessage);
		assertEquals(ContextClosedEvent.class, closedEventMessage.getPayload().getClass());
	}


	private static class TestApplicationEvent1 extends ApplicationEvent {

		public TestApplicationEvent1() {
			super("event1");
		}
	}


	private static class TestApplicationEvent2 extends ApplicationEvent {

		public TestApplicationEvent2() {
			super("event2");
		}
	}

}
