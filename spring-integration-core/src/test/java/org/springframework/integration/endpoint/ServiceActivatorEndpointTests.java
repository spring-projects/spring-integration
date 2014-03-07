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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class ServiceActivatorEndpointTests {

	@Test
	public void outputChannel() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test
	public void outputChannelTakesPrecedence() {
		QueueChannel channel1 = new QueueChannel(1);
		QueueChannel channel2 = new QueueChannel(1);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		endpoint.setOutputChannel(channel1);
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(channel2).build();
		endpoint.handleMessage(message);
		Message<?> reply1 = channel1.receive(0);
		assertNotNull(reply1);
		assertEquals("FOO", reply1.getPayload());
		Message<?> reply2 = channel2.receive(0);
		assertNull(reply2);
	}

	@Test
	public void returnAddressHeader() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(channel).build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test
	public void returnAddressHeaderWithChannelName() {
		QueueChannel channel = new QueueChannel(1);
		channel.setBeanName("testChannel");
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("testChannel", channel);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		endpoint.setChannelResolver(channelResolver);
		Message<?> message = MessageBuilder.withPayload("foo")
				.setReplyChannelName("testChannel").build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test
	public void dynamicReplyChannel() throws Exception {
		final QueueChannel replyChannel1 = new QueueChannel();
		final QueueChannel replyChannel2 = new QueueChannel();
		replyChannel2.setBeanName("replyChannel2");
		Object handler = new Object() {
			@SuppressWarnings("unused")
			public Message<?> handle(Message<?> message) {
				return new GenericMessage<String>("foo" + message.getPayload());
			}
		};
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(handler, "handle");
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("replyChannel2", replyChannel2);
		endpoint.setChannelResolver(channelResolver);
		Message<String> testMessage1 = MessageBuilder.withPayload("bar")
				.setReplyChannel(replyChannel1).build();
		endpoint.handleMessage(testMessage1);
		Message<?> reply1 = replyChannel1.receive(50);
		assertNotNull(reply1);
		assertEquals("foobar", reply1.getPayload());
		Message<?> reply2 = replyChannel2.receive(0);
		assertNull(reply2);
		Message<String> testMessage2 = MessageBuilder.fromMessage(testMessage1)
				.setReplyChannelName("replyChannel2").build();
		endpoint.handleMessage(testMessage2);
		reply1 = replyChannel1.receive(0);
		assertNull(reply1);
		reply2 = replyChannel2.receive(0);
		assertNotNull(reply2);
		assertEquals("foobar", reply2.getPayload());
	}

	@Test
	public void noOutputChannelFallsBackToReturnAddress() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(channel).build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void noReplyTarget() {
		ServiceActivatingHandler endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
	}

	@Test
	public void noReplyMessage() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(
				new TestNullReplyBean(), "handle");
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
		assertNull(channel.receive(0));
	}

	@Test(expected = ReplyRequiredException.class)
	public void noReplyMessageWithRequiresReply() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(
				new TestNullReplyBean(), "handle");
		endpoint.setRequiresReply(true);
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
	}

	@Test
	public void correlationIdNotSetIfMessageIsReturnedUnaltered() {
		QueueChannel replyChannel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(new Object() {
			@SuppressWarnings("unused")
			public Message<?> handle(Message<?> message) {
				return message;
			}
		}, "handle");
		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		endpoint.handleMessage(message);
		Message<?> reply = replyChannel.receive(500);
		assertNull(new IntegrationMessageHeaderAccessor(reply).getCorrelationId());
	}

	@Test
	public void correlationIdSetByHandlerTakesPrecedence() {
		QueueChannel replyChannel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(new Object() {
			@SuppressWarnings("unused")
			public Message<?> handle(Message<?> message) {
				return MessageBuilder.fromMessage(message)
						.setCorrelationId("ABC-123").build();
			}
		}, "handle");
		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		endpoint.handleMessage(message);
		Message<?> reply = replyChannel.receive(500);
		Object correlationId = new IntegrationMessageHeaderAccessor(reply).getCorrelationId();
		assertFalse(message.getHeaders().getId().equals(correlationId));
		assertEquals("ABC-123", correlationId);
	}

	@Test
	public void testBeanFactoryPopulation() {
		ServiceActivatingHandler endpoint = this.createEndpoint();
		BeanFactory mock = Mockito.mock(BeanFactory.class);
		endpoint.setBeanFactory(mock);
		endpoint.afterPropertiesSet();
		Object beanFactory = TestUtils.getPropertyValue(endpoint, "processor.beanFactory");
		assertNotNull(beanFactory);
		assertSame(mock, beanFactory);
	}


	private ServiceActivatingHandler createEndpoint() {
		 return new ServiceActivatingHandler(new TestBean(), "handle");
	}


	private static class TestBean {

		@SuppressWarnings("unused")
		public Message<?> handle(Message<?> message) {
			return new GenericMessage<String>(message.getPayload().toString().toUpperCase());
		}
	}


	private static class TestNullReplyBean {

		@SuppressWarnings("unused")
		public Message<?> handle(Message<?> message) {
			return null;
		}
	}

}
