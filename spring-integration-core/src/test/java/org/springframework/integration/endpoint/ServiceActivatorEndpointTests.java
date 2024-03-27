/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.junit.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class ServiceActivatorEndpointTests {

	@Test
	public void outputChannel() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = createEndpoint();
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("FOO");
	}

	@Test
	public void outputChannelTakesPrecedence() {
		QueueChannel channel1 = new QueueChannel(1);
		QueueChannel channel2 = new QueueChannel(1);
		ServiceActivatingHandler endpoint = createEndpoint();
		endpoint.setOutputChannel(channel1);
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(channel2).build();
		endpoint.handleMessage(message);
		Message<?> reply1 = channel1.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("FOO");
		Message<?> reply2 = channel2.receive(0);
		assertThat(reply2).isNull();
	}

	@Test
	public void returnAddressHeader() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(channel).build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("FOO");
	}

	@Test
	public void returnAddressHeaderWithChannelName() {
		TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();
		testApplicationContext.refresh();
		QueueChannel channel = new QueueChannel(1);
		channel.setBeanName("testChannel");
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel("testChannel", channel);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		endpoint.setChannelResolver(channelResolver);
		endpoint.setBeanFactory(testApplicationContext);
		endpoint.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("foo")
				.setReplyChannelName("testChannel").build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("FOO");
		testApplicationContext.close();
	}

	@Test
	public void dynamicReplyChannel() throws Exception {
		TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();
		testApplicationContext.refresh();
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
		endpoint.setBeanFactory(testApplicationContext);
		endpoint.afterPropertiesSet();
		Message<String> testMessage1 = MessageBuilder.withPayload("bar")
				.setReplyChannel(replyChannel1).build();
		endpoint.handleMessage(testMessage1);
		Message<?> reply1 = replyChannel1.receive(50);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("foobar");
		Message<?> reply2 = replyChannel2.receive(0);
		assertThat(reply2).isNull();
		Message<String> testMessage2 = MessageBuilder.fromMessage(testMessage1)
				.setReplyChannelName("replyChannel2").build();
		endpoint.handleMessage(testMessage2);
		reply1 = replyChannel1.receive(0);
		assertThat(reply1).isNull();
		reply2 = replyChannel2.receive(0);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("foobar");
		testApplicationContext.close();
	}

	@Test
	public void noOutputChannelFallsBackToReturnAddress() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(channel).build();
		endpoint.handleMessage(message);
		Message<?> reply = channel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("FOO");
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
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(new TestNullReplyBean(), "handle");
		endpoint.setOutputChannel(channel);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
		assertThat(channel.receive(0)).isNull();
	}

	@Test(expected = ReplyRequiredException.class)
	public void noReplyMessageWithRequiresReply() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingHandler endpoint = new ServiceActivatingHandler(new TestNullReplyBean(), "handle");
		endpoint.setRequiresReply(true);
		endpoint.setOutputChannel(channel);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.handleMessage(message);
	}

	@Test
	public void correlationIdNotSetIfMessageIsReturnedUnaltered() {
		QueueChannel replyChannel = new QueueChannel(1);
		ServiceActivatingHandler endpoint =
				new ServiceActivatingHandler(new Object() {

					@SuppressWarnings("unused")
					public Message<?> handle(Message<?> message) {
						return message;
					}
				}, "handle");
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();

		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		endpoint.handleMessage(message);
		Message<?> reply = replyChannel.receive(500);
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId()).isNull();
	}

	@Test
	public void correlationIdSetByHandlerTakesPrecedence() {
		QueueChannel replyChannel = new QueueChannel(1);
		ServiceActivatingHandler endpoint =
				new ServiceActivatingHandler(new Object() {

					@SuppressWarnings("unused")
					public Message<?> handle(Message<?> message) {
						return MessageBuilder.fromMessage(message)
								.setCorrelationId("ABC-123").build();
					}
				}, "handle");
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();

		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		endpoint.handleMessage(message);
		Message<?> reply = replyChannel.receive(500);
		Object correlationId = new IntegrationMessageHeaderAccessor(reply).getCorrelationId();
		assertThat(correlationId).isNotEqualTo(message.getHeaders().getId());
		assertThat(correlationId).isEqualTo("ABC-123");
	}

	@Test
	public void testBeanFactoryPopulation() {
		ServiceActivatingHandler endpoint = this.createEndpoint();
		BeanFactory mock = mock(BeanFactory.class);
		endpoint.setBeanFactory(mock);
		endpoint.afterPropertiesSet();
		Object beanFactory = TestUtils.getPropertyValue(endpoint, "processor.beanFactory");
		assertThat(beanFactory).isNotNull();
		assertThat(beanFactory).isSameAs(mock);
	}

	private ServiceActivatingHandler createEndpoint() {
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new TestBean(), "handle");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		return handler;
	}

	private static class TestBean {

		@SuppressWarnings("unused")
		public Message<?> handle(Message<?> message) {
			return new GenericMessage<>(message.getPayload().toString().toUpperCase());
		}

	}

	private static class TestNullReplyBean {

		@SuppressWarnings("unused")
		public Message<?> handle(Message<?> message) {
			return null;
		}

	}

}
