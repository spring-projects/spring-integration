/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.event.outbound;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Ma Jiandong
 */
public class ApplicationEventPublishingMessageHandlerTests {

	@Test
	public void messagingEvent() {
		TestApplicationEventPublisher publisher = new TestApplicationEventPublisher();
		ApplicationEventPublishingMessageHandler handler = new ApplicationEventPublishingMessageHandler();
		handler.setApplicationEventPublisher(publisher);
		assertThat(publisher.getLastEvent()).isNull();
		Message<?> message = new GenericMessage<>("testing");
		handler.handleMessage(message);
		Object event = publisher.getLastEvent();
		assertThat(event.getClass()).isEqualTo(MessagingEvent.class);
		assertThat(((MessagingEvent) event).getMessage()).isEqualTo(message);
	}

	@Test
	public void payloadAsEvent() {
		TestApplicationEventPublisher publisher = new TestApplicationEventPublisher();
		ApplicationEventPublishingMessageHandler handler = new ApplicationEventPublishingMessageHandler();
		handler.setApplicationEventPublisher(publisher);
		assertThat(publisher.getLastEvent()).isNull();
		Message<?> message = new GenericMessage<>(new TestEvent("foo"));
		handler.handleMessage(message);
		Object event = publisher.getLastEvent();
		assertThat(event.getClass()).isEqualTo(TestEvent.class);
		assertThat(((ApplicationEvent) event).getSource()).isEqualTo("foo");
	}

	@Test
	public void payloadAsIs() {
		TestApplicationEventPublisher publisher = new TestApplicationEventPublisher();
		ApplicationEventPublishingMessageHandler handler = new ApplicationEventPublishingMessageHandler();
		handler.setApplicationEventPublisher(publisher);
		handler.setPublishPayload(true);
		assertThat(publisher.getLastEvent()).isNull();
		Message<?> message = new GenericMessage<>("testing");
		handler.handleMessage(message);
		Object event = publisher.getLastEvent();
		assertThat(event.getClass()).isEqualTo(String.class);
		assertThat(((String) event)).isEqualTo("testing");
	}

	private static class TestApplicationEventPublisher implements ApplicationEventPublisher {

		private volatile Object lastEvent;

		public Object getLastEvent() {
			return lastEvent;
		}

		@Override
		public void publishEvent(Object event) {
			this.lastEvent = event;
		}

	}

	@SuppressWarnings("serial")
	private static class TestEvent extends ApplicationEvent {

		TestEvent(String text) {
			super(text);
		}

	}

}
