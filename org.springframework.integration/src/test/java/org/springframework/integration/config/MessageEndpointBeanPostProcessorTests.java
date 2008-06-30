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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.CommandMessage;
import org.springframework.integration.message.PollCommand;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageEndpointBeanPostProcessorTests {

	@Test
	public void testNoProxyCreatedForHandlerEndpointWithEmptyAdviceChain() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageEndpointBeanPostProcessorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerEndpointWithoutAdvice");
		assertFalse(AopUtils.isAopProxy(endpoint));
	}

	@Test
	public void testHandlerEndpointWithAdviceChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageEndpointBeanPostProcessorTests.xml", this.getClass());
		context.start();
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerEndpointWithAdvice");
		assertTrue(AopUtils.isAopProxy(endpoint));
		TestBeforeAdvice beforeAdvice = (TestBeforeAdvice) context.getBean("simpleAdvice");
		TestEndpointInterceptor interceptor = (TestEndpointInterceptor) context.getBean("interceptor");
		assertEquals(0, beforeAdvice.getCount());
		assertEquals(0, interceptor.getCount());
		endpoint.invoke(new StringMessage("test"));
		assertEquals(1, beforeAdvice.getCount());
		assertEquals(2, interceptor.getCount());
		context.stop();
	}

	@Test
	public void testNoProxyCreatedForTargetEndpointWithEmptyAdviceChain() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageEndpointBeanPostProcessorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("targetEndpointWithoutAdvice");
		assertFalse(AopUtils.isAopProxy(endpoint));
	}

	@Test
	public void testTargetEndpointWithAdviceChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageEndpointBeanPostProcessorTests.xml", this.getClass());
		context.start();
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("targetEndpointWithAdvice");
		assertTrue(AopUtils.isAopProxy(endpoint));
		TestBeforeAdvice beforeAdvice = (TestBeforeAdvice) context.getBean("simpleAdvice");
		TestEndpointInterceptor interceptor = (TestEndpointInterceptor) context.getBean("interceptor");
		assertEquals(0, beforeAdvice.getCount());
		assertEquals(0, interceptor.getCount());
		endpoint.invoke(new StringMessage("test"));
		assertEquals(1, beforeAdvice.getCount());
		assertEquals(2, interceptor.getCount());
		context.stop();
	}

	@Test
	public void testNoProxyCreatedForSourceEndpointWithEmptyAdviceChain() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageEndpointBeanPostProcessorTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("sourceEndpointWithoutAdvice");
		assertFalse(AopUtils.isAopProxy(endpoint));
	}

	@Test
	public void testSourceEndpointWithAdviceChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"messageEndpointBeanPostProcessorTests.xml", this.getClass());
		context.start();
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("sourceEndpointWithAdvice");
		assertTrue(AopUtils.isAopProxy(endpoint));
		TestBeforeAdvice beforeAdvice = (TestBeforeAdvice) context.getBean("simpleAdvice");
		TestEndpointInterceptor interceptor = (TestEndpointInterceptor) context.getBean("interceptor");
		assertEquals(0, beforeAdvice.getCount());
		assertEquals(0, interceptor.getCount());
		endpoint.invoke(new CommandMessage(new PollCommand()));
		assertEquals(1, beforeAdvice.getCount());
		assertEquals(2, interceptor.getCount());
		context.stop();
	}

}
