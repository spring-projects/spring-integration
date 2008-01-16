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

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.Subscription;
import org.springframework.integration.handler.TestHandlers;

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
		assertEquals(context.getBean("errorMessages"), bus.getInvalidMessageChannel());
	}

	@Test
	public void testDefaultErrorChannel() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.initialize();
		assertNotNull("bus should have created a default error channel", bus.getInvalidMessageChannel());
	}

	@Test(expected=MessagingConfigurationException.class)
	public void testAutoCreateChannelsDisabledByDefault() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		Subscription subscription = new Subscription("unknownChannel");
		bus.registerHandler("handler", TestHandlers.nullHandler(), subscription);
		bus.start();
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

}
