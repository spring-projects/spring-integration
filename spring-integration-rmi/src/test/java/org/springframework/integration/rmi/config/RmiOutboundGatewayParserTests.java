/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.rmi.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.rmi.RmiOutboundGateway;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RmiOutboundGatewayParserTests {

	private final QueueChannel testChannel = new QueueChannel();

	@Before
	public void setupTestInboundGateway() throws Exception {
		testChannel.setBeanName("testChannel");
		RmiInboundGateway gateway = new RmiInboundGateway();
		gateway.setRequestChannel(testChannel);
		gateway.setExpectReply(false);
		gateway.afterPropertiesSet();
	}

	@Test
	public void testOrder() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		RmiOutboundGateway gateway = context.getBean(RmiOutboundGateway.class);
		assertEquals(23, TestUtils.getPropertyValue(gateway, "order"));
	}

	@Test
	public void directInvocation() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel localChannel = (MessageChannel) context.getBean("localChannel");
		localChannel.send(new GenericMessage<String>("test"));
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void endpointInvocation() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel localChannel = (MessageChannel) context.getBean("localChannel");
		localChannel.send(new GenericMessage<String>("test"));
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

}
