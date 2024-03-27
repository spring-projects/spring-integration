/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InnerBeanConfigTests {

	@Autowired
	private EventDrivenConsumer testEndpoint;

	@Autowired
	private ApplicationContext context;

	// INT-1528: the inner bean should not be registered in the context
	@Test(expected = NoSuchBeanDefinitionException.class)
	public void checkInnerBean() {
		Object innerBean = TestUtils.getPropertyValue(testEndpoint, "handler.processor.delegate.targetObject");
		assertThat(innerBean).isNotNull();
		context.getBean(TestBean.class);
	}

	public static class TestBean {

		public String echo(String value) {
			return value;
		}

	}

}
