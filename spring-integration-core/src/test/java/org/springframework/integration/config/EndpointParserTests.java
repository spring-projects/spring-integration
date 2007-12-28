/*
 * Copyright 2002-2007 the original author or authors.
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
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 */
public class EndpointParserTests {

	@Test
	public void testSimpleEndpoint() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"genericEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		TestHandler handler = (TestHandler) context.getBean("testHandler");
		assertNull(handler.getMessageString());
		channel.send(new GenericMessage<String>(1, "test"));
		handler.getLatch().await(50, TimeUnit.MILLISECONDS);
		assertEquals("test", handler.getMessageString());
	}

	@Test
	public void testHandlerAdapterEndpoint() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"handlerAdapterEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		TestBean bean = (TestBean) context.getBean("testBean");
		assertNull(bean.getMessage());
		channel.send(new GenericMessage<String>(1, "test"));
		bean.getLatch().await(500, TimeUnit.MILLISECONDS);
		assertEquals("test", bean.getMessage());
	}

}
