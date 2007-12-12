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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 */
public class ComponentConfigurerTests {

	@Test
	public void testServiceActivator() {
		GenericApplicationContext context = new GenericApplicationContext();
		ComponentConfigurer cr = new ComponentConfigurer(context, null);
		context.registerBeanDefinition("tester", new RootBeanDefinition(Tester.class));
		context.registerBeanDefinition("out", new RootBeanDefinition(PointToPointChannel.class));
		context.registerBeanDefinition("bus", new RootBeanDefinition(MessageBus.class));
		String name = cr.serviceActivator(null, "out", "tester", "test");
		context.refresh();
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean(name);
		endpoint.messageReceived(new GenericMessage<String>(1, "world"));
		MessageChannel channel = (MessageChannel) context.getBean("out");
		assertEquals("hello world", channel.receive().getPayload());
	}

	@Test
	public void testInboundChannelAdapter() {
		GenericApplicationContext context = new GenericApplicationContext();
		ComponentConfigurer cr = new ComponentConfigurer(context, null);
		context.registerBeanDefinition("tester", new RootBeanDefinition(Tester.class));
		String adapter = cr.inboundChannelAdapter("tester", "foo");
		MessageChannel channel = (MessageChannel) context.getBean(adapter);
		Message<String> message = channel.receive();
		assertEquals("bar", message.getPayload());
	}

	@Test
	public void testOutboundChannelAdapter() {
		GenericApplicationContext context = new GenericApplicationContext();
		ComponentConfigurer cr = new ComponentConfigurer(context, null);
		context.registerBeanDefinition("tester", new RootBeanDefinition(Tester.class));
		String adapter = cr.outboundChannelAdapter("tester", "store");
		Tester tester = (Tester) context.getBean("tester");
		assertNull(tester.getStoredValue());
		MessageChannel channel = (MessageChannel) context.getBean(adapter);
		boolean result = channel.send(new GenericMessage<String>(1, "foo"));
		assertTrue(result);
		assertEquals("foo", tester.getStoredValue());
	}


	public static class Tester {

		private String storedValue;

		public String test(String s) {
			return "hello " + s;
		}

		public String foo() {
			return "bar";
		}

		public void store(String s) {
			this.storedValue = s;
		}

		public String getStoredValue() {
			return this.storedValue;
		}
	}
}
