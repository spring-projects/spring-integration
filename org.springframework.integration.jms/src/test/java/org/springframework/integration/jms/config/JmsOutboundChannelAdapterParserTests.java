/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import javax.jms.DeliveryMode;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jms.JmsHeaderMapper;

/**
 * @author Mark Fisher
 */
public class JmsOutboundChannelAdapterParserTests {

	@Test
	public void adapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithConnectionFactoryAndDestination.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertNotNull(accessor.getPropertyValue("jmsTemplate"));
	}

	@Test
	public void adapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithConnectionFactoryAndDestinationName.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertNotNull(accessor.getPropertyValue("jmsTemplate"));
	}

	@Test
	public void adapterWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithDefaultConnectionFactory.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertNotNull(accessor.getPropertyValue("jmsTemplate"));
	}

	@Test
	public void adapterWithOrder() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithOrder.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		Object order = accessor.getPropertyValue("order");
		assertEquals(123, order);
	}

	@Test
	public void adapterWithHeaderMapper() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithHeaderMapper.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		JmsHeaderMapper headerMapper = (JmsHeaderMapper) accessor.getPropertyValue("headerMapper");
		assertNotNull(headerMapper);
		assertEquals(TestJmsHeaderMapper.class, headerMapper.getClass());
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void adapterWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("jmsOutboundWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			assertTrue(e.getMessage().contains("connection-factory"));
			throw e;
		}
	}

	@Test
	public void adapterWithQosSettings() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithQos.xml", this.getClass());
		EventDrivenConsumer endpoint = context.getBean("qosAdapter", EventDrivenConsumer.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(new DirectFieldAccessor(endpoint).getPropertyValue("handler"))
						.getPropertyValue("jmsTemplate"));
		assertEquals(true, accessor.getPropertyValue("explicitQosEnabled"));
		assertEquals(12345L, accessor.getPropertyValue("timeToLive"));
		assertEquals(7, accessor.getPropertyValue("priority"));
		assertEquals(DeliveryMode.NON_PERSISTENT, accessor.getPropertyValue("deliveryMode"));
	}

	@Test
	public void qosNotExplicitByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithQos.xml", this.getClass());
		EventDrivenConsumer endpoint = context.getBean("defaultAdapter", EventDrivenConsumer.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(new DirectFieldAccessor(endpoint).getPropertyValue("handler"))
						.getPropertyValue("jmsTemplate"));
		assertEquals(false, accessor.getPropertyValue("explicitQosEnabled"));
	}

}
