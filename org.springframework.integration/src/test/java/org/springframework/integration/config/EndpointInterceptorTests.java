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

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EndpointPoller;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class EndpointInterceptorTests {

	@Test
	public void testHandlerEndpointWithBeanInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerEndpointWithBeanInterceptors");
		testInterceptors(endpoint, context, true);
	}

	@Test
	public void testHandlerEndpointWithRefInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerEndpointWithRefInterceptors");
		testInterceptors(endpoint, context, false);
	}

	@Test
	public void testTargetEndpointWithBeanInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("targetEndpointWithBeanInterceptors");
		testInterceptors(endpoint, context, true);
	}

	@Test
	public void testTargetEndpointWithRefInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("targetEndpointWithRefInterceptors");
		testInterceptors(endpoint, context, false);
	}

	@Test
	public void testSourceEndpointWithBeanInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("sourceEndpointWithBeanInterceptors");
		testInterceptors(endpoint, context, true);
	}

	@Test
	public void testSourceEndpointWithRefInterceptors() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"endpointInterceptorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("sourceEndpointWithRefInterceptors");
		testInterceptors(endpoint, context, false);
	}


	private static void testInterceptors(MessageEndpoint endpoint, ClassPathXmlApplicationContext context, boolean innerBeans) {
		TestPreSendInterceptor preInterceptor = null;
		TestAroundSendEndpointInterceptor aroundInterceptor = null;
		if (innerBeans) {
			preInterceptor = (TestPreSendInterceptor) ((AbstractEndpoint) endpoint).getInterceptors().get(0);
			aroundInterceptor = (TestAroundSendEndpointInterceptor) ((AbstractEndpoint) endpoint).getInterceptors().get(1);
		}
		else {
			preInterceptor = (TestPreSendInterceptor) context.getBean("preInterceptor");
			aroundInterceptor = (TestAroundSendEndpointInterceptor) context.getBean("aroundInterceptor");
		}
		assertEquals(0, preInterceptor.getCount());
		assertEquals(0, aroundInterceptor.getCount());
		if (endpoint instanceof SourceEndpoint) {
			MessageChannel channel = (MessageChannel) context.getBean("testChannel");
			channel.send(new StringMessage("foo"));
			endpoint.send(new GenericMessage<EndpointPoller>(new EndpointPoller()));
		}
		else {
			endpoint.send(new StringMessage("test"));
		}
		assertEquals(1, preInterceptor.getCount());
		assertEquals(2, aroundInterceptor.getCount());
		context.stop();
	}

}
