/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class PresenceOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	private static volatile int adviceCalled;

	@Test
	public void testRosterEventOutboundChannelAdapterParserAsPollingConsumer() {
		Object pollingConsumer = context.getBean("pollingOutboundRosterAdapter");
		assertThat(pollingConsumer instanceof PollingConsumer).isTrue();
		AbstractXmppConnectionAwareMessageHandler handler = TestUtils.getPropertyValue(pollingConsumer, "handler");
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(23);
	}

	@Test
	public void testRosterEventOutboundChannelAdapterParserEventConsumer() {
		Object eventConsumer = context.getBean("eventOutboundRosterAdapter");
		assertThat(eventConsumer instanceof EventDrivenConsumer).isTrue();
		AbstractXmppConnectionAwareMessageHandler handler = TestUtils.getPropertyValue(eventConsumer, "handler");
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(34);
	}

	@Test
	public void advised() {
		Object eventConsumer = context.getBean("advised");
		assertThat(eventConsumer instanceof EventDrivenConsumer).isTrue();
		MessageHandler handler = TestUtils.getPropertyValue(eventConsumer, "handler");
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testRosterEventOutboundChannel() {
		Object channel = context.getBean("eventOutboundRosterChannel");
		assertThat(channel instanceof SubscribableChannel).isTrue();
		UnicastingDispatcher dispatcher = TestUtils.getPropertyValue(channel, "dispatcher");
		Set<MessageHandler> handlers = TestUtils.getPropertyValue(dispatcher, "handlers");
		assertThat(TestUtils.<Integer>getPropertyValue(handlers.toArray()[0], "order")).isEqualTo(45);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
