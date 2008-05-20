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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class HandlerParserTests {

	@Test
	public void testTopLevelHandlerAdapter() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"handlerAdapterParserTests.xml", HandlerParserTests.class);
		MessageHandler adapter = (MessageHandler) context.getBean("handlerAdapter");
		assertNotNull(adapter);
		Message<?> reply = adapter.handle(new StringMessage("foo"));
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());
	}

	@Test
	public void testHandlerChain() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"handlerChainParserTests.xml", HandlerParserTests.class);
		TestBean testBean = (TestBean) context.getBean("testBean");
		assertNull(testBean.getMessage());
		MessageHandler handlerChain = (MessageHandler) context.getBean("handlerChain");
		assertNotNull(handlerChain);
		Message<?> reply = handlerChain.handle(new StringMessage("test"));
		assertNotNull(reply);
		assertEquals(0, testBean.getLatch().getCount());
		assertEquals("foo", testBean.getMessage());
		assertEquals("bar", reply.getPayload());
	}

}
