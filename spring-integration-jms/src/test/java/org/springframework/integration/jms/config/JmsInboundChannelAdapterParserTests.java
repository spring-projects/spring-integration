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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class JmsInboundChannelAdapterParserTests {

	long timeoutOnReceive = 3000;

	@Test
	public void adapterWithJmsTemplate() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithJmsTemplate.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(timeoutOnReceive);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundAdapter", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("jms:inbound-channel-adapter", componentHistoryRecord.get("type"));
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
		context.close();
	}

	@Test
	public void adapterWithoutJmsTemplateAndAcknowlegeMode() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithJmsTemplate.xml", this.getClass());
		JmsTemplate jmsTemplate =
			TestUtils.getPropertyValue(context.getBean("inboundAdapterWithoutJmsTemplate"), "source.jmsTemplate", JmsTemplate.class);
		assertTrue(jmsTemplate.isSessionTransacted());
		context.stop();
		context.close();
	}

	@Test
	public void adapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithConnectionFactoryAndDestination.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		assertFalse(TestUtils.getPropertyValue(context.getBean("adapter"), "source.jmsTemplate", JmsTemplate.class)
				.isSessionTransacted());
		context.stop();
		context.close();
	}

	@Test
	public void adapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithConnectionFactoryAndDestinationName.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
		context.close();
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void adapterWithConnectionFactoryOnly() {
		new ClassPathXmlApplicationContext("jmsInboundWithConnectionFactoryOnly.xml", this.getClass()).close();
	}

	@Test(expected = BeanCreationException.class)
	public void adapterWithDestinationOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsInboundWithDestinationOnly.xml", this.getClass());
		}
		catch (BeanCreationException e) {
			Throwable rootCause = e.getRootCause();
			assertEquals(NoSuchBeanDefinitionException.class, rootCause.getClass());
			throw e;
		}
	}

	@Test
	public void adpaterWithDestinationAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithDestinationAndDefaultConnectionFactory.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
		context.close();
	}

	@Test(expected=BeanCreationException.class)
	public void adapterWithDestinationNameOnly() {
		new ClassPathXmlApplicationContext("jmsInboundWithDestinationNameOnly.xml", this.getClass()).close();
	}

	@Test
	public void adapterWithDestinationNameAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
					"jmsInboundWithDestinationNameAndDefaultConnectionFactory.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
		context.close();
	}

	@Test
	public void adapterWithHeaderMapper() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithHeaderMapper.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		assertEquals("foo", message.getHeaders().get("testProperty"));
		assertEquals(new Integer(123), message.getHeaders().get("testAttribute"));
		context.stop();
		context.close();
	}

	@Test
	public void adapterWithMessageSelector() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageSelector.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output1");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("test [with selector: TestProperty = 'foo']", message.getPayload());
		context.stop();
		context.close();
	}

	@Test
	public void pollingAdapterWithReceiveTimeout() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithReceiveTimeout.xml", this.getClass());
		JmsTemplate jmsTemplate =
				TestUtils.getPropertyValue(context.getBean("adapter"), "source.jmsTemplate", JmsTemplate.class);
		assertEquals(99, jmsTemplate.getReceiveTimeout());
		context.close();
	}

	@Test
	public void pollingAdapterWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageConverter.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output1");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("converted-test", message.getPayload());
		context.stop();
		context.close();
	}

	@Test
	public void messageDrivenAdapterWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageConverter.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output2");
		Message<?> message = output.receive(timeoutOnReceive);
		assertNotNull("message should not be null", message);
		assertEquals("converted-test", message.getPayload());
		context.stop();
		context.close();
	}

}
