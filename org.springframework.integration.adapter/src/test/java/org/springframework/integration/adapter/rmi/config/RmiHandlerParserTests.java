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

package org.springframework.integration.adapter.rmi.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.rmi.RmiGateway;
import org.springframework.integration.adapter.rmi.RmiHandler;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RmiHandlerParserTests {

	private final QueueChannel testChannel = new QueueChannel();


	@Before
	public void exportRemoteHandler() throws Exception {
		testChannel.setBeanName("testChannel");
		RmiGateway gateway = new RmiGateway(testChannel);
		gateway.setExpectReply(false);
		gateway.afterPropertiesSet();
	}

	@Test
	public void testRmiHandlerDirectly() {
		ApplicationContext context = new ClassPathXmlApplicationContext("rmiHandlerParserTests.xml", this.getClass());
		RmiHandler handler = (RmiHandler) context.getBean("handler");
		handler.handle(new StringMessage("test"));
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void testRmiHandlerWithEndpoint() {
		ApplicationContext context = new ClassPathXmlApplicationContext("rmiHandlerParserTests.xml", this.getClass());
		MessageChannel localChannel = (MessageChannel) context.getBean("localChannel");
		localChannel.send(new StringMessage("test"));
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

}
