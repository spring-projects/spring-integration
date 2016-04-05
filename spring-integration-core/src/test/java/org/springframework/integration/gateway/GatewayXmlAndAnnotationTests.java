/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayXmlAndAnnotationTests {

	@Autowired
	GatewayProxyFactoryBean gatewayProxyFactoryBean;

	@Test
	public void test() {
		assertEquals(123L, TestUtils.getPropertyValue(gatewayProxyFactoryBean, "defaultReplyTimeout"));
		@SuppressWarnings("unchecked")
		Map<Method, MessagingGatewaySupport> gatewayMap = TestUtils.getPropertyValue(gatewayProxyFactoryBean,
				"gatewayMap", Map.class);
		int assertions = 0;
		for (Entry<Method, MessagingGatewaySupport> entry : gatewayMap.entrySet()) {
			if (entry.getKey().getName().equals("annotationShouldntOverrideDefault")) {
				assertEquals(123L, TestUtils.getPropertyValue(entry.getValue(),
						"replyTimeout"));
				assertions++;
			}
			else if (entry.getKey().getName().equals("annotationShouldOverrideDefault")) {
				assertEquals(234L, TestUtils.getPropertyValue(entry.getValue(),
						"replyTimeout"));
				assertions++;
			}
			else if (entry.getKey().getName().equals("annotationShouldOverrideDefaultToInfinity")) {
				assertEquals(-1L, TestUtils.getPropertyValue(entry.getValue(),
						"replyTimeout"));
				assertions++;
			}
			else if (entry.getKey().getName().equals("explicitTimeoutShouldOverrideDefault")) {
				assertEquals(456L, TestUtils.getPropertyValue(entry.getValue(),
						"replyTimeout"));
				assertions++;
			}
		}
		assertEquals(4, assertions);
	}

	public interface AGateway {
		@Gateway
		String annotationShouldntOverrideDefault(String foo);

		@Gateway(replyTimeout = 234)
		String annotationShouldOverrideDefault(String foo);

		@Gateway(replyTimeout = -1)
		String annotationShouldOverrideDefaultToInfinity(String foo);

		String explicitTimeoutShouldOverrideDefault(String foo);
	}
}
