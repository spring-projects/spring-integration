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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.scheduling.Subscription;

/**
 * @author Mark Fisher
 */
public class MessageBusParserTests {

	@Test
	public void testErrorChannelReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithErrorChannelReference.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.initialize();
		assertEquals(context.getBean("testErrorChannel"), bus.getErrorChannel());
	}

	@Test
	public void testDefaultErrorChannel() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.initialize();
		assertNotNull("bus should have created a default error channel", bus.getErrorChannel());
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testAutoCreateChannelsDisabledByDefault() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		Subscription subscription = new Subscription("unknownChannel");
		bus.registerHandler("handler", TestHandlers.nullHandler(), subscription);
	}

	@Test
	public void testAutoCreateChannelsEnabled() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithAutoCreateChannels.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		Subscription subscription = new Subscription("channelToCreate");
		bus.registerHandler("handler", TestHandlers.nullHandler(), subscription);
		bus.start();
		assertNotNull(bus.lookupChannel("channelToCreate"));
		bus.stop();
	}

	@Test
	public void testMultipleMessageBusElements() {
		boolean exceptionThrown = false;
		try {
			new ClassPathXmlApplicationContext("multipleMessageBusElements.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			exceptionThrown = true;
			assertEquals(MessagingConfigurationException.class, e.getCause().getClass());
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testMessageBusElementAndBean() {
		boolean exceptionThrown = false;
		try {
			new ClassPathXmlApplicationContext("messageBusElementAndBean.xml", this.getClass());
		}
		catch (BeanCreationException e) {
			exceptionThrown = true;
			assertEquals(MessagingConfigurationException.class, e.getCause().getClass());
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testAutoStartup() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithAutoStartup.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		assertTrue(bus.isRunning());
		bus.stop();
	}

	@Test
	public void testDefaultConcurrency() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaultConcurrencyTests.xml", this.getClass());
		MessageEndpoint endpoint1 = (MessageEndpoint) context.getBean("endpoint1");
		assertEquals(4, endpoint1.getConcurrencyPolicy().getCoreSize());
		assertEquals(7, endpoint1.getConcurrencyPolicy().getMaxSize());
	}

	@Test
	public void testExplicitConcurrencyTakesPrecedence() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaultConcurrencyTests.xml", this.getClass());
		MessageEndpoint endpoint2 = (MessageEndpoint) context.getBean("endpoint2");
		assertEquals(14, endpoint2.getConcurrencyPolicy().getCoreSize());
		assertEquals(17, endpoint2.getConcurrencyPolicy().getMaxSize());	
	}

}
