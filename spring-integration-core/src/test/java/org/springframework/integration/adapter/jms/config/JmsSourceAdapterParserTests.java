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

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.jms.JmsMessageDrivenSourceAdapter;
import org.springframework.integration.adapter.jms.JmsPollingSourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class JmsSourceAdapterParserTests {

	@Test
	public void testPollingAdapterWithJmsTemplate() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithJmsTemplate.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.dispatch();
		Message<?> message = channel.receive(100);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testPollingAdapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithConnectionFactoryAndDestination.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.dispatch();
		Message<?> message = channel.receive(100);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testPollingAdapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingAdapterWithConnectionFactoryAndDestinationName.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		JmsPollingSourceAdapter adapter = (JmsPollingSourceAdapter) context.getBean("adapter");
		adapter.dispatch();
		Message<?> message = channel.receive(100);
		assertNotNull("message should not be null", message);
		assertEquals("polling-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageDrivenAdapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithConnectionFactoryAndDestination.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		JmsMessageDrivenSourceAdapter adapter = (JmsMessageDrivenSourceAdapter) context.getBean("adapter");
		assertEquals(JmsMessageDrivenSourceAdapter.class, adapter.getClass());
		Message<?> message = channel.receive(100);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testMessageDrivenAdapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageDrivenAdapterWithConnectionFactoryAndDestinationName.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		JmsMessageDrivenSourceAdapter adapter = (JmsMessageDrivenSourceAdapter) context.getBean("adapter");
		assertEquals(JmsMessageDrivenSourceAdapter.class, adapter.getClass());
		Message<?> message = channel.receive(100);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
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

	@Test(expected=BeanDefinitionStoreException.class)
	public void testPollingAdapterWithDestinationOnly() {
		try {
			new ClassPathXmlApplicationContext("pollingAdapterWithDestinationOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testPollingAdapterWithDestinationNameOnly() {
		try {
			new ClassPathXmlApplicationContext("pollingAdapterWithDestinationNameOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
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

}
