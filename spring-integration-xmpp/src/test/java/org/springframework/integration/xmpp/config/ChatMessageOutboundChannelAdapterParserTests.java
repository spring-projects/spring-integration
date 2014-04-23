/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.integration.xmpp.support.XmppHeaderMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Florian Schmaus
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChatMessageOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private XmppHeaderMapper headerMapper;

	private static volatile int adviceCalled;

	@Test
	public void testPollingConsumer() {
		Object pollingConsumer = context.getBean("withHeaderMapper");
		QueueChannel channel = (QueueChannel) TestUtils.getPropertyValue(pollingConsumer, "inputChannel");
		assertEquals("outboundPollingChannel", channel.getComponentName());
		assertTrue(pollingConsumer instanceof PollingConsumer);
	}

	@Test
	public void testEventConsumerWithNoChannel() {
		Object eventConsumer = context.getBean("outboundNoChannelAdapter");
		assertTrue(eventConsumer instanceof SubscribableChannel);
	}

	@Test
	public void advised() {
		MessageHandler handler = TestUtils.getPropertyValue(context.getBean("advised"),
				"handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEventConsumer() {
		Object eventConsumer = context.getBean("outboundEventAdapter");
		DefaultXmppHeaderMapper headerMapper =
				TestUtils.getPropertyValue(eventConsumer, "handler.headerMapper", DefaultXmppHeaderMapper.class);
		List<String> requestHeaderNames = TestUtils.getPropertyValue(headerMapper, "requestHeaderNames", List.class);
		assertEquals(2, requestHeaderNames.size());
		assertEquals("foo*", requestHeaderNames.get(0));
		assertEquals("bar*", requestHeaderNames.get(1));
		assertTrue(eventConsumer instanceof EventDrivenConsumer);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapper() throws Exception{
		Object pollingConsumer = context.getBean("withHeaderMapper");
		assertTrue(pollingConsumer instanceof PollingConsumer);
		assertEquals(headerMapper, TestUtils.getPropertyValue(pollingConsumer, "handler.headerMapper"));
		MessageChannel channel = context.getBean("outboundEventChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader(XmppHeaders.TO, "oleg").
				setHeader("foobar", "foobar").build();
		XMPPConnection connection = context.getBean("testConnection", XMPPConnection.class);

		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.jivesoftware.smack.packet.Message xmppMessage = (org.jivesoftware.smack.packet.Message) args[0];
		          assertEquals("oleg", xmppMessage.getTo());
		          assertEquals("foobar", JivePropertiesManager.getProperty(xmppMessage, "foobar"));
		          return null;
		      }})
		  .when(connection).sendPacket(Mockito.any(org.jivesoftware.smack.packet.Message.class));

		channel.send(message);

		verify(connection, times(1)).sendPacket(Mockito.any(org.jivesoftware.smack.packet.Message.class));
		Mockito.reset(connection);
	}

	@SuppressWarnings("rawtypes")
	@Test //INT-2275
	public void testOutboundChannelAdapterInsideChain() throws Exception{
		MessageChannel channel = context.getBean("outboundChainChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader(XmppHeaders.TO, "artem").build();
		XMPPConnection connection = context.getBean("testConnection", XMPPConnection.class);
		Mockito.doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				org.jivesoftware.smack.packet.Message xmppMessage = (org.jivesoftware.smack.packet.Message) args[0];
				assertEquals("artem", xmppMessage.getTo());
				assertEquals("hello", xmppMessage.getBody());
				return null;
			}})
				.when(connection).sendPacket(Mockito.any(org.jivesoftware.smack.packet.Message.class));

		channel.send(message);

		verify(connection, times(1)).sendPacket(Mockito.any(org.jivesoftware.smack.packet.Message.class));
		Mockito.reset(connection);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
