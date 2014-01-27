/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.context;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationContextTests {

	@Autowired
	@Qualifier(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
	private Properties integrationProperties;

	@Autowired
	@Qualifier("fooService")
	private IntegrationObjectSupport serviceActivator;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Test
	public void testIntegrationContextComponents() {
		assertEquals("true", this.integrationProperties.get(IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY));
		assertEquals("20", this.integrationProperties.get(IntegrationProperties.TASK_SCHEDULER_POOL_SIZE));
		assertEquals(this.integrationProperties, this.serviceActivator.getIntegrationProperties());
		assertEquals(20, TestUtils.getPropertyValue(this.taskScheduler, "poolSize"));
	}

}
