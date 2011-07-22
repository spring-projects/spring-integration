/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.amqp.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @since 2.1
 */
public class OutboundGatewayTests {

	private XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource(getClass().getSimpleName()+"-context.xml", getClass()));

	@Test
	public void testVanillaConfiguration() throws Exception {
		assertTrue(beanFactory.containsBeanDefinition("vanilla"));
		beanFactory.getBean("vanilla");
	}

	@Test
	public void testExpressionBasedConfiguration() throws Exception {
		assertTrue(beanFactory.containsBeanDefinition("expression"));
		Object target = beanFactory.getBean("expression");
		assertNotNull(ReflectionTestUtils.getField(ReflectionTestUtils.getField(target, "handler"), "routingKeyGenerator"));
	}

}
