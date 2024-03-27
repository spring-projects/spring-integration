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
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class CorrelationIdTests {

	@Test
	public void testCorrelationIdPassedIfAvailable() {
		Object correlationId = "123-ABC";
		Message<String> message = MessageBuilder.withPayload("test")
				.setCorrelationId(correlationId).build();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel(1);
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(new TestBean(), "upperCase");
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, serviceActivator);
		endpoint.start();
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = outputChannel.receive(0);
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId()).isEqualTo(correlationId);
	}

	@Test
	public void testCorrelationIdCopiedFromMessageCorrelationIdIfAvailable() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setCorrelationId("correlationId").build();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel(1);
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(new TestBean(), "upperCase");
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, serviceActivator);
		endpoint.start();
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = outputChannel.receive(0);
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId())
				.isEqualTo(new IntegrationMessageHeaderAccessor(message).getCorrelationId());
	}

	@Test
	public void testCorrelationNotPassedFromRequestHeaderIfAlreadySetByHandler() {
		Object correlationId = "123-ABC";
		Message<String> message = MessageBuilder.withPayload("test")
				.setCorrelationId(correlationId).build();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel(1);
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(new TestBean(), "createMessage");
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, serviceActivator);
		endpoint.start();
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = outputChannel.receive(0);
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId()).isEqualTo("456-XYZ");
	}

	@Test
	public void testCorrelationNotCopiedFromRequestMessgeIdIfAlreadySetByHandler() throws Exception {
		Message<?> message = new GenericMessage<String>("test");
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel(1);
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(new TestBean(), "createMessage");
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, serviceActivator);
		endpoint.start();
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = outputChannel.receive(0);
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId()).isEqualTo("456-XYZ");
	}

	@Test
	public void testCorrelationIdWithSplitterWhenNotValueSetOnIncomingMessage() throws Exception {
		Message<?> message = new GenericMessage<String>("test1,test2");
		QueueChannel testChannel = new QueueChannel();
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(
				new TestBean(), TestBean.class.getMethod("split", String.class));
		splitter.setOutputChannel(testChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		Message<?> reply1 = testChannel.receive(100);
		Message<?> reply2 = testChannel.receive(100);
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void testCorrelationIdWithSplitterWhenValueSetOnIncomingMessage() throws Exception {
		final String correlationIdForTest = "#FOR_TEST#";
		Message<?> message = MessageBuilder.withPayload("test1,test2").setCorrelationId(correlationIdForTest).build();
		QueueChannel testChannel = new QueueChannel();
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(
				new TestBean(), TestBean.class.getMethod("split", String.class));
		splitter.setOutputChannel(testChannel);
		splitter.setBeanFactory(mock(BeanFactory.class));
		splitter.afterPropertiesSet();
		splitter.handleMessage(message);
		Message<?> reply1 = testChannel.receive(100);
		Message<?> reply2 = testChannel.receive(100);
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
		assertThat(reply1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS))
				.as("Sequence details missing").isTrue();
		assertThat(reply2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS))
				.as("Sequence details missing").isTrue();
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		TestBean() {
			super();
		}

		public String upperCase(String input) {
			return input.toUpperCase();
		}

		public String[] split(String input) {
			return input.split(",");
		}

		public Message<?> createMessage(String input) {
			return MessageBuilder.withPayload(input).setCorrelationId("456-XYZ").build();
		}

	}

}
