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

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.MessageHeaderMapper;
import org.springframework.integration.jms.JmsTarget;

/**
 * @author Mark Fisher
 */
public class JmsTargetParserTests {

	@Test
	public void testTargetWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetWithConnectionFactoryAndDestination.xml", this.getClass());
		JmsTarget target = (JmsTarget) context.getBean("target");
		DirectFieldAccessor accessor = new DirectFieldAccessor(target);
		assertNotNull(accessor.getPropertyValue("jmsTemplate"));
	}

	@Test
	public void testTargetWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetWithConnectionFactoryAndDestinationName.xml", this.getClass());
		JmsTarget target = (JmsTarget) context.getBean("target");
		DirectFieldAccessor accessor = new DirectFieldAccessor(target);
		assertNotNull(accessor.getPropertyValue("jmsTemplate"));
	}

	@Test
	public void testTargetWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetWithDefaultConnectionFactory.xml", this.getClass());
		JmsTarget target = (JmsTarget) context.getBean("target");
		DirectFieldAccessor accessor = new DirectFieldAccessor(target);
		assertNotNull(accessor.getPropertyValue("jmsTemplate"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTargetWithHeaderMapper() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"targetWithHeaderMapper.xml", this.getClass());
		JmsTarget target = (JmsTarget) context.getBean("target");
		DirectFieldAccessor accessor = new DirectFieldAccessor(target);
		MessageHeaderMapper headerMapper = (MessageHeaderMapper)
				accessor.getPropertyValue("headerMapper");
		assertNotNull(headerMapper);
		assertEquals(TestMessageHeaderMapper.class, headerMapper.getClass());
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testTargetWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("targetWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

}
