/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xmpp.config;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.integration.xmpp.support.XmppHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Florian Schmaus
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class ChatMessageOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private XmppHeaderMapper headerMapper;

	@Autowired
	private ExtensionElementProvider<?> extensionElementProvider;

	private static volatile int adviceCalled;

	@Test
	public void testPollingConsumer() {
		Object pollingConsumer = context.getBean("withHeaderMapper");
		QueueChannel channel = TestUtils.getPropertyValue(pollingConsumer, "inputChannel");
		assertThat(channel.getComponentName()).isEqualTo("outboundPollingChannel");
		assertThat(pollingConsumer).isInstanceOf(PollingConsumer.class);
	}

	@Test
	public void testEventConsumerWithNoChannel() {
		Object eventConsumer = context.getBean("outboundNoChannelAdapter");
		assertThat(eventConsumer).isInstanceOf(SubscribableChannel.class);
	}

	@Test
	public void advised() {
		MessageHandler handler = TestUtils.getPropertyValue(context.getBean("advised"), "handler");
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testEventConsumer() {
		Object eventConsumer = context.getBean("outboundEventAdapter");
		DefaultXmppHeaderMapper headerMapper = TestUtils.getPropertyValue(eventConsumer, "handler.headerMapper");

		AbstractHeaderMapper.HeaderMatcher requestHeaderMatcher = TestUtils.getPropertyValue(headerMapper,
				"requestHeaderMatcher");
		assertThat(requestHeaderMatcher.matchHeader("foo")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("foo123")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("bar")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("bar123")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("biz")).isFalse();
		assertThat(requestHeaderMatcher.matchHeader("else")).isFalse();
		assertThat(eventConsumer).isInstanceOf(EventDrivenConsumer.class);

		MessageHandler outboundEventAdapterHandle =
				context.getBean("outboundEventAdapter.handler", MessageHandler.class);
		assertThat(TestUtils.<Object>getPropertyValue(outboundEventAdapterHandle, "extensionProvider"))
				.isSameAs(this.extensionElementProvider);
	}

	@Test
	public void withHeaderMapper() throws Exception {
		Object pollingConsumer = context.getBean("withHeaderMapper");
		assertThat(pollingConsumer instanceof PollingConsumer).isTrue();
		assertThat(TestUtils.<DefaultXmppHeaderMapper>getPropertyValue(pollingConsumer, "handler.headerMapper"))
				.isEqualTo(headerMapper);
		MessageChannel channel = context.getBean("outboundEventChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader(XmppHeaders.TO, "oleg").
				setHeader("foobar", "foobar").build();
		XMPPConnection connection = context.getBean("testConnection", XMPPConnection.class);

		doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			org.jivesoftware.smack.packet.Message xmppMessage = (org.jivesoftware.smack.packet.Message) args[0];
			assertThat(xmppMessage.getTo().toString()).isEqualTo("oleg");
			assertThat(JivePropertiesManager.getProperty(xmppMessage, "foobar")).isEqualTo("foobar");
			return null;
		}).when(connection).sendStanza(Mockito.any(org.jivesoftware.smack.packet.Message.class));

		channel.send(message);

		verify(connection, times(1)).sendStanza(Mockito.any(org.jivesoftware.smack.packet.Message.class));
		Mockito.reset(connection);
	}

	@Test //INT-2275
	public void testOutboundChannelAdapterInsideChain() throws Exception {
		MessageChannel channel = context.getBean("outboundChainChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader(XmppHeaders.TO, "artem").build();
		XMPPConnection connection = context.getBean("testConnection", XMPPConnection.class);
		doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			org.jivesoftware.smack.packet.Message xmppMessage = (org.jivesoftware.smack.packet.Message) args[0];
			assertThat(xmppMessage.getTo().toString()).isEqualTo("artem");
			assertThat(xmppMessage.getBody()).isEqualTo("hello");
			return null;
		}).when(connection).sendStanza(Mockito.any(org.jivesoftware.smack.packet.Message.class));

		channel.send(message);

		verify(connection, times(1)).sendStanza(Mockito.any(org.jivesoftware.smack.packet.Message.class));
		Mockito.reset(connection);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
