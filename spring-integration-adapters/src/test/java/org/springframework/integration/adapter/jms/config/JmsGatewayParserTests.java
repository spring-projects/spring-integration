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
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.jms.JmsGateway;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class JmsGatewayParserTests {

	@Test
	public void testGatewayWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithConnectionFactoryAndDestination.xml", this.getClass());
		MessageChannel channel = new QueueChannel(1);
		JmsGateway source = (JmsGateway) context.getBean("jmsSource");
		source.setRequestChannel(channel);
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithConnectionFactoryAndDestinationName.xml", this.getClass());
		MessageChannel channel = new QueueChannel(1);
		JmsGateway source = (JmsGateway) context.getBean("jmsSource");
		source.setRequestChannel(channel);
		context.start();
		assertEquals(JmsGateway.class, source.getClass());
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithMessageConverter.xml", this.getClass());
		MessageChannel channel = new QueueChannel(1);
		JmsGateway source = (JmsGateway) context.getBean("jmsSource");
		source.setRequestChannel(channel);
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("converted-test-message", message.getPayload());
		context.stop();
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testGatewayWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("messageDrivenAdapterWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testGatewayWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("messageDrivenAdapterWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test
	public void testGatewayWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithDefaultConnectionFactory.xml", this.getClass());
		MessageChannel channel = new QueueChannel(1);
		JmsGateway source = (JmsGateway) context.getBean("jmsSource");
		source.setRequestChannel(channel);
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

}
