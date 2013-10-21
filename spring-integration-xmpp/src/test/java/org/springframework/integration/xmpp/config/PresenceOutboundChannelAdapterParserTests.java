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

package org.springframework.integration.xmpp.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareMessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PresenceOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	private static volatile int adviceCalled;

	@Test
	public void testRosterEventOutboundChannelAdapterParserAsPollingConsumer(){
		Object pollingConsumer = context.getBean("pollingOutboundRosterAdapter");
		assertTrue(pollingConsumer instanceof PollingConsumer);
		AbstractXmppConnectionAwareMessageHandler handler = (AbstractXmppConnectionAwareMessageHandler) TestUtils
				.getPropertyValue(pollingConsumer, "handler");
		assertEquals(23, TestUtils.getPropertyValue(handler, "order"));
	}

	@Test
	public void testRosterEventOutboundChannelAdapterParserEventConsumer(){
		Object eventConsumer = context.getBean("eventOutboundRosterAdapter");
		assertTrue(eventConsumer instanceof EventDrivenConsumer);
		AbstractXmppConnectionAwareMessageHandler handler = (AbstractXmppConnectionAwareMessageHandler) TestUtils
				.getPropertyValue(eventConsumer, "handler");
		assertEquals(34, TestUtils.getPropertyValue(handler, "order"));
	}

	@Test
	public void advised(){
		Object eventConsumer = context.getBean("advised");
		assertTrue(eventConsumer instanceof EventDrivenConsumer);
		MessageHandler handler = TestUtils.getPropertyValue(eventConsumer, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testRosterEventOutboundChannel(){
		Object channel = context.getBean("eventOutboundRosterChannel");
		assertTrue(channel instanceof SubscribableChannel);
		UnicastingDispatcher dispatcher = (UnicastingDispatcher) TestUtils
				.getPropertyValue(channel, "dispatcher");
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(dispatcher, "handlers");
		assertEquals(45, TestUtils.getPropertyValue(handlers.toArray()[0], "order"));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
