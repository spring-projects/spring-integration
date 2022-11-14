/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jms.config;

import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsInboundChannelAdapterParserTests {

	private final long timeoutOnReceive = 20000;

	@Test
	public void adapterWithJmsTemplate() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithJmsTemplate.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output");
			Message<?> message = output.receive(timeoutOnReceive);
			MessageHistory history = MessageHistory.read(message);
			assertThat(history).isNotNull();
			Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundAdapter", 0);
			assertThat(componentHistoryRecord).isNotNull();
			assertThat(componentHistoryRecord.get("type")).isEqualTo("jms:inbound-channel-adapter");
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("polling-test");
		}
	}

	@Test
	public void adapterWithoutJmsTemplateAndAcknowledgeMode() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithJmsTemplate.xml", this.getClass())) {

			JmsTemplate jmsTemplate =
					TestUtils.getPropertyValue(context.getBean("inboundAdapterWithoutJmsTemplate"),
							"source.jmsTemplate", JmsTemplate.class);
			assertThat(jmsTemplate.isSessionTransacted()).isTrue();
		}
	}

	@Test
	public void adapterWithConnectionFactoryAndDestination() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithConnectionFactoryAndDestination.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("polling-test");
			assertThat(TestUtils.getPropertyValue(context.getBean("adapter"), "source.jmsTemplate", JmsTemplate.class)
					.isSessionTransacted()).isFalse();
		}
	}

	@Test
	public void adapterWithConnectionFactoryAndDestinationName() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithConnectionFactoryAndDestinationName.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output");
			Message<?> message = output.receive(this.timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("polling-test");
			JmsDestinationPollingSource jmsDestinationPollingSource = context
					.getBean(JmsDestinationPollingSource.class);
			JmsTemplate jmsTemplate =
					TestUtils.getPropertyValue(jmsDestinationPollingSource, "jmsTemplate", JmsTemplate.class);
			assertThat(jmsTemplate.getReceiveTimeout()).isEqualTo(1000);
		}
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void adapterWithConnectionFactoryOnly() {
		new ClassPathXmlApplicationContext("jmsInboundWithConnectionFactoryOnly.xml", this.getClass()).close();
	}

	@Test(expected = BeanCreationException.class)
	public void adapterWithDestinationOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsInboundWithDestinationOnly.xml", this.getClass()).close();
		}
		catch (BeanCreationException e) {
			Throwable rootCause = e.getRootCause();
			assertThat(rootCause.getClass()).isEqualTo(NoSuchBeanDefinitionException.class);
			throw e;
		}
	}

	@Test
	public void testAdapterWithDestinationAndDefaultConnectionFactory() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithDestinationAndDefaultConnectionFactory.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("polling-test");
		}
	}

	@Test(expected = BeanCreationException.class)
	public void adapterWithDestinationNameOnly() {
		new ClassPathXmlApplicationContext("jmsInboundWithDestinationNameOnly.xml", this.getClass()).close();
	}

	@Test
	public void adapterWithDestinationNameAndDefaultConnectionFactory() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithDestinationNameAndDefaultConnectionFactory.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("polling-test");
		}
	}

	@Test
	public void adapterWithHeaderMapper() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithHeaderMapper.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("polling-test");
			assertThat(message.getHeaders().get("testProperty")).isEqualTo("foo");
			assertThat(message.getHeaders().get("testAttribute")).isEqualTo(123);
		}
	}

	@Test
	public void adapterWithMessageSelector() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageSelector.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output1");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("test [with selector: TestProperty = 'foo']");
		}
	}

	@Test
	public void pollingAdapterWithReceiveTimeout() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithReceiveTimeout.xml", this.getClass())) {

			JmsTemplate jmsTemplate =
					TestUtils.getPropertyValue(context.getBean("adapter"), "source.jmsTemplate", JmsTemplate.class);
			assertThat(jmsTemplate.getReceiveTimeout()).isEqualTo(99);
		}
	}

	@Test
	public void pollingAdapterWithMessageConverter() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageConverter.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output1");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("converted-test");
		}
	}

	@Test
	public void messageDrivenAdapterWithMessageConverter() {
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageConverter.xml", this.getClass())) {

			PollableChannel output = (PollableChannel) context.getBean("output2");
			Message<?> message = output.receive(timeoutOnReceive);
			assertThat(message).as("message should not be null").isNotNull();
			assertThat(message.getPayload()).isEqualTo("converted-test");
		}
	}

}
