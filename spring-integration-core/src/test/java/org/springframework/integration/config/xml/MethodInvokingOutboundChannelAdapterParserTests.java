/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MethodInvokingOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void checkConfig() {
		Object adapter = context.getBean("adapter");
		Object handler = TestUtils.getPropertyValue(adapter, "handler");
		assertEquals(MethodInvokingMessageHandler.class, handler.getClass());
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(99, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(adapter, "autoStartup"));
	}
	
	@Test
	public void checkConfigWithInnerBeanAndPoller() {
		Object adapter = context.getBean("adapterB");
		Object handler = TestUtils.getPropertyValue(adapter, "handler");
		assertEquals(MethodInvokingMessageHandler.class, handler.getClass());
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(99, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(adapter, "autoStartup"));
	}

 /*   @Test
	public void checkConfigWithExpression() {
		Object adapter = context.getBean("adapterC");
		Object handler = TestUtils.getPropertyValue(adapter, "handler");
		assertEquals(ExpressionEvaluatingMessageProcessor.class, handler.getClass());
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(99, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(adapter, "autoStartup"));
	}
*/
	static class TestBean {

		public void out(Object o) {
		}

	}
}
