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

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.jms.JmsTargetAdapter;
import org.springframework.integration.endpoint.TargetEndpoint;

/**
 * @author Mark Fisher
 */
public class JmsTargetAdapterParserTests {

	@Test
	public void testTargetAdapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetAdapterWithConnectionFactoryAndDestination.xml", this.getClass());
		TargetEndpoint endpoint = (TargetEndpoint) context.getBean("adapter");
		assertEquals(JmsTargetAdapter.class, endpoint.getTarget().getClass());
		assertEquals("adapter", endpoint.getName());
		assertEquals("testChannel", endpoint.getSubscription().getChannelName());
	}

	@Test
	public void testTargetAdapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetAdapterWithConnectionFactoryAndDestinationName.xml", this.getClass());
		TargetEndpoint endpoint = (TargetEndpoint) context.getBean("adapter");
		assertEquals(JmsTargetAdapter.class, endpoint.getTarget().getClass());
		assertEquals("adapter", endpoint.getName());
		assertEquals("testChannel", endpoint.getSubscription().getChannelName());
	}

	@Test
	public void testTargetAdapterWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetAdapterWithDefaultConnectionFactory.xml", this.getClass());
		TargetEndpoint endpoint = (TargetEndpoint) context.getBean("adapter");
		assertEquals(JmsTargetAdapter.class, endpoint.getTarget().getClass());
		assertEquals("adapter", endpoint.getName());
		assertEquals("testChannel", endpoint.getSubscription().getChannelName());
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testTargetAdapterWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("targetAdapterWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

}
