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

package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Header;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayWithHeaderAnnotations {

	@Autowired
	private ApplicationContext applicationContext;


	@Test // INT-1205
	public void priorityAsArgument() {
		TestService gateway = (TestService) applicationContext.getBean("gateway");
		String result = gateway.test("foo", 99, "bar");
		assertEquals("foo99bar", result);
	}


	public static interface TestService {
		// wrt INT-1205, priority no longer has a $ prefix, so here we are testing the $custom header as well
		public String test(String str, @Header(IntegrationMessageHeaderAccessor.PRIORITY) int priority, @Header("$custom") String custom);
	}

}
