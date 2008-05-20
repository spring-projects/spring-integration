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

package org.springframework.integration.adapter.httpinvoker.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.httpinvoker.HttpInvokerTargetAdapter;
import org.springframework.integration.endpoint.HandlerEndpoint;

/**
 * @author Mark Fisher
 */
public class HttpInvokerTargetAdapterParserTests {

	@Test
	public void testHttpInvokerTargetAdapter() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerTargetAdapterParserTests.xml", this.getClass());
		HandlerEndpoint endpoint = (HandlerEndpoint) context.getBean("adapter");
		assertNotNull(endpoint);
		assertEquals(HttpInvokerTargetAdapter.class, endpoint.getHandler().getClass());
		assertEquals("testChannel", endpoint.getSubscription().getChannelName());
	}

}
