/*
 * Copyright 2014-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.config;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
		assertThat(TestUtils.getPropertyValue(fb.getObject(), "readDelay")).isEqualTo(100L);
	}

	@Test
	public void testReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setReadDelay(1000);
		fb.setBeanFactory(mock(BeanFactory.class));
		fb.afterPropertiesSet();
		assertThat(TestUtils.getPropertyValue(fb.getObject(), "readDelay")).isEqualTo(1000L);
	}

}
