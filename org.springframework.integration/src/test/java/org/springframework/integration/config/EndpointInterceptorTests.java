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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EndpointInterceptor;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class EndpointInterceptorTests {

	@Test
	public void testHandlerEndpointWithBeanInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("endpointWithBeanInterceptors");
		testInterceptors(endpoint, context, true);
	}

	@Test
	public void testHandlerEndpointWithRefInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("endpointWithRefInterceptors");
		testInterceptors(endpoint, context, false);
	}


	@SuppressWarnings("unchecked")
	private static void testInterceptors(MessageEndpoint endpoint, ClassPathXmlApplicationContext context, boolean innerBeans) {
		TestPreHandleInterceptor preInterceptor = null;
		TestPostHandleInterceptor postInterceptor = null;
		if (innerBeans) {
			DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
			List<EndpointInterceptor> interceptors = (List<EndpointInterceptor>) accessor.getPropertyValue("interceptors");
			preInterceptor = (TestPreHandleInterceptor) interceptors.get(0);
			postInterceptor = (TestPostHandleInterceptor) interceptors.get(1);
		}
		else {
			preInterceptor = (TestPreHandleInterceptor) context.getBean("preInterceptor");
			postInterceptor = (TestPostHandleInterceptor) context.getBean("postInterceptor");
		}
		assertEquals(0, preInterceptor.getCount());
		assertEquals(0, postInterceptor.getCount());
		endpoint.send(new StringMessage("test"));
		assertEquals(1, preInterceptor.getCount());
		assertEquals(1, postInterceptor.getCount());
		context.stop();
	}

}
