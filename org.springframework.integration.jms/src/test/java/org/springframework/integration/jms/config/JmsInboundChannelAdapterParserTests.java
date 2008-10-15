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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;

/**
 * @author Mark Fisher
 */
public class JmsInboundChannelAdapterParserTests {

	@Test
	public void adapterWithJmsTemplate() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithJmsTemplate.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
	}

	@Test
	public void adapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithConnectionFactoryAndDestination.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void adapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithConnectionFactoryAndDestinationName.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void adapterWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsInboundWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected = BeanCreationException.class)
	public void adapterWithDestinationOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsInboundWithDestinationOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(NoSuchBeanDefinitionException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test
	public void adpaterWithDestinationAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithDestinationAndDefaultConnectionFactory.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanCreationException.class)
	public void adapterWithDestinationNameOnly() {
		new ClassPathXmlApplicationContext("jmsInboundWithDestinationNameOnly.xml", this.getClass());
	}

	@Test
	public void adapterWithDestinationNameAndDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
					"jmsInboundWithDestinationNameAndDefaultConnectionFactory.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
	}

	@Test
	public void adapterWithHeaderMapper() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithHeaderMapper.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output");
		Message<?> message = output.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		assertEquals("foo", message.getHeaders().get("testProperty"));
		assertEquals(new Integer(123), message.getHeaders().get("testAttribute"));
	}

}
