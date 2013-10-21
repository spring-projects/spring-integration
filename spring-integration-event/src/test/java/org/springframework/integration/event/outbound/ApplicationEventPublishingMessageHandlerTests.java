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

package org.springframework.integration.event.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class ApplicationEventPublishingMessageHandlerTests {

	@Test
	public void messagingEvent() throws InterruptedException {
		TestApplicationEventPublisher publisher = new TestApplicationEventPublisher();
		ApplicationEventPublishingMessageHandler handler = new ApplicationEventPublishingMessageHandler();
		handler.setApplicationEventPublisher(publisher);
		assertNull(publisher.getLastEvent());
		Message<?> message = new GenericMessage<String>("testing");
		handler.handleMessage(message);
		ApplicationEvent event = publisher.getLastEvent();
		assertEquals(MessagingEvent.class, event.getClass());
		assertEquals(message, ((MessagingEvent) event).getMessage());
	}

	@Test
	public void payloadAsEvent() {
		TestApplicationEventPublisher publisher = new TestApplicationEventPublisher();
		ApplicationEventPublishingMessageHandler handler = new ApplicationEventPublishingMessageHandler();
		handler.setApplicationEventPublisher(publisher);
		assertNull(publisher.getLastEvent());
		Message<?> message = new GenericMessage<TestEvent>(new TestEvent("foo"));
		handler.handleMessage(message);
		ApplicationEvent event = publisher.getLastEvent();
		assertEquals(TestEvent.class, event.getClass());
		assertEquals("foo", ((TestEvent) event).getSource());	
	}


	private static class TestApplicationEventPublisher implements ApplicationEventPublisher {

		private volatile ApplicationEvent lastEvent;

		public ApplicationEvent getLastEvent() {
			return this.lastEvent;
		}

		public void publishEvent(ApplicationEvent event) {
			this.lastEvent = event;
		}
	}


	@SuppressWarnings("serial")
	private static class TestEvent extends ApplicationEvent {

		public TestEvent(String text) {
			super(text);
		}
	}

}
