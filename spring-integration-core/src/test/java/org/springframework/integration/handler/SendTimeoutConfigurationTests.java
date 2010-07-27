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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SendTimeoutConfigurationTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void serviceActivator() {
		assertEquals(123, this.getTimeout("serviceActivator"));
	}

	@Test
	public void filter() {
		assertEquals(123, this.getTimeout("filter"));
	}

	@Test
	public void transformer() {
		assertEquals(123, this.getTimeout("transformer"));
	}

	@Test
	public void splitter() {
		assertEquals(123, this.getTimeout("splitter"));
	}

	@Test
	public void router() {
		assertEquals(123, this.getTimeout("router"));
	}


	private long getTimeout(String endpointName) {
		return TestUtils.getPropertyValue(context.getBean(endpointName),
				"handler.messagingTemplate.sendTimeout", Long.class).longValue();
	}

}
