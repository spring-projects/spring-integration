/*
 * Copyright 2002-2008 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.jms.JmsSource;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.PollingSourceEndpoint;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class JmsSourceParserTests {

	@Test
	public void testSourceWithJmsTemplate() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsSourceWithJmsTemplate.xml", this.getClass());
		JmsSource source = (JmsSource) context.getBean("jmsSource");
		Message<?> message = source.receive();
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
	}

	@Test
	public void testSourceWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsSourceWithConnectionFactoryAndDestination.xml", this.getClass());
		JmsSource source = (JmsSource) context.getBean("jmsSource");
		Message<?> message = source.receive();
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testSourceWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsSourceWithConnectionFactoryAndDestinationName.xml", this.getClass());
		JmsSource source = (JmsSource) context.getBean("jmsSource");
		Message<?> message = source.receive();
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testSourceWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsSourceWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanCreationException.class)
	public void testSourceWithDestinationOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsSourceWithDestinationOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(NoSuchBeanDefinitionException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test
	public void testSourceWithDestinationAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsSourceWithDestinationAndDefaultConnectionFactory.xml", this.getClass());
		JmsSource source = (JmsSource) context.getBean("jmsSource");
		Message<?> message = source.receive();
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanCreationException.class)
	public void testSourceWithDestinationNameOnly() {
		new ClassPathXmlApplicationContext("jmsSourceWithDestinationNameOnly.xml", this.getClass());
	}

	@Test
	public void testSourceWithDestinationNameAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
					"jmsSourceWithDestinationNameAndDefaultConnectionFactory.xml", this.getClass());
		JmsSource source = (JmsSource) context.getBean("jmsSource");
		Message<?> message = source.receive();
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
	}

	@Test
	public void testSourceEndpoint() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsSourceEndpoint.xml", this.getClass());
		context.start();
		PollingSourceEndpoint endpoint = (PollingSourceEndpoint) context.getBean("endpoint");
		assertEquals(JmsSource.class, endpoint.getSource().getClass());
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

}
