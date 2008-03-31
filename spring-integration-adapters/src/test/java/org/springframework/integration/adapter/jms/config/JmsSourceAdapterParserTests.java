/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.jms.JmsMessageDrivenSourceAdapter;
import org.springframework.integration.adapter.jms.JmsPollingSourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * @author Mark Fisher
 */
public class JmsSourceAdapterParserTests {

	@Test
	public void testPollingAdapterWithJmsTemplate() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithJmsTemplate.xml", this.getClass());
		context.start();
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.processMessages();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(500);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testPollingAdapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithConnectionFactoryAndDestination.xml", this.getClass());
		context.start();
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.processMessages();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(500);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testPollingAdapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithConnectionFactoryAndDestinationName.xml", this.getClass());
		context.start();
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.processMessages();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(500);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageDrivenAdapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithConnectionFactoryAndDestination.xml", this.getClass());
		context.start();
		JmsMessageDrivenSourceAdapter adapter = (JmsMessageDrivenSourceAdapter) context.getBean("adapter");
		assertEquals(JmsMessageDrivenSourceAdapter.class, adapter.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageDrivenAdapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithConnectionFactoryAndDestinationName.xml", this.getClass());
		context.start();
		JmsMessageDrivenSourceAdapter adapter = (JmsMessageDrivenSourceAdapter) context.getBean("adapter");
		assertEquals(JmsMessageDrivenSourceAdapter.class, adapter.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageDrivenAdapterWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithMessageConverter.xml", this.getClass());
		JmsMessageDrivenSourceAdapter adapter = (JmsMessageDrivenSourceAdapter) context.getBean("adapter");
		assertEquals(JmsMessageDrivenSourceAdapter.class, adapter.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("converted-test-message", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testPollingAdapterWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("pollingAdapterWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanCreationException.class)
	public void testPollingAdapterWithDestinationOnly() {
		try {
			new ClassPathXmlApplicationContext("pollingAdapterWithDestinationOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test
	public void testPollingAdapterWithDestinationAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithDestinationAndDefaultConnectionFactory.xml", this.getClass());
		context.start();
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.processMessages();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(500);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanCreationException.class)
	public void testPollingAdapterWithDestinationNameOnly() {
		new ClassPathXmlApplicationContext("pollingAdapterWithDestinationNameOnly.xml", this.getClass());
	}

	@Test
	public void testPollingAdapterWithDestinationNameAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
					"pollingAdapterWithDestinationNameAndDefaultConnectionFactory.xml", this.getClass());
		context.start();
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.processMessages();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(500);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testPollingAdapterWithoutPollPeriod() {
		try {
			new ClassPathXmlApplicationContext("pollingAdapterWithoutPollPeriod.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testMessageDrivenAdapterWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("messageDrivenAdapterWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testMessageDrivenAdapterWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("messageDrivenAdapterWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test
	public void testMessageDrivenAdapterWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithDefaultConnectionFactory.xml", this.getClass());
		context.start();
		JmsMessageDrivenSourceAdapter adapter = (JmsMessageDrivenSourceAdapter) context.getBean("adapter");
		assertEquals(JmsMessageDrivenSourceAdapter.class, adapter.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}


	public static class TestMessageConverter implements MessageConverter {

		public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
			String original = ((TextMessage) message).getText();
			return "converted-" + original;
		}

		public javax.jms.Message toMessage(Object object, Session session) throws JMSException,
				MessageConversionException {
			return null;
		}

	}

}
