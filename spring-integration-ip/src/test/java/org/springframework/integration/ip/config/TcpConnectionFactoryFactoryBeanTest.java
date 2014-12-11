/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.ip.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.1.1
 */
public class TcpConnectionFactoryFactoryBeanTest {

	@Test
	public void testNoReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setBeanFactory(mock(BeanFactory.class));
		fb.afterPropertiesSet();
		// INT-3578 IllegalArgumentException on 'readDelay'
		assertEquals(100L, TestUtils.getPropertyValue(fb.getObject(), "readDelay"));
	}



	@Test
	public void testReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setReadDelay(1000);
		fb.setBeanFactory(mock(BeanFactory.class));
		fb.afterPropertiesSet();
		assertEquals(1000L, TestUtils.getPropertyValue(fb.getObject(), "readDelay"));
	}

}
