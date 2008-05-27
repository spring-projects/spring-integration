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

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.rmi.RmiGateway;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.gateway.RequestReplyTemplate;

/**
 * @author Mark Fisher
 */
public class RmiGatewayParserTests {

	@Test
	public void testAdapterWithDefaults() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiGatewayParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		RmiGateway gateway = (RmiGateway) context.getBean("gatewayWithDefaults");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(true, accessor.getPropertyValue("expectReply"));
		RequestReplyTemplate template = (RequestReplyTemplate)
				accessor.getPropertyValue("requestReplyTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(channel, templateAccessor.getPropertyValue("requestChannel"));
		assertEquals(-1L, templateAccessor.getPropertyValue("requestTimeout"));
		assertEquals(-1L, templateAccessor.getPropertyValue("replyTimeout"));
	}

	@Test
	public void testAdapterWithCustomProperties() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiGatewayParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		RmiGateway gateway = (RmiGateway) context.getBean("gatewayWithCustomProperties");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(false, accessor.getPropertyValue("expectReply"));
		RequestReplyTemplate template = (RequestReplyTemplate)
				accessor.getPropertyValue("requestReplyTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(channel, templateAccessor.getPropertyValue("requestChannel"));
		assertEquals(123L, templateAccessor.getPropertyValue("requestTimeout"));
		assertEquals(456L, templateAccessor.getPropertyValue("replyTimeout"));
	}

	@Test
	public void testAdapterWithHost() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiGatewayParserTests.xml", this.getClass());
		RmiGateway gateway = (RmiGateway) context.getBean("gatewayWithHost");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals("localhost", accessor.getPropertyValue("registryHost"));
	}

	@Test
	public void testAdapterWithPort() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiGatewayParserTests.xml", this.getClass());
		RmiGateway gateway = (RmiGateway) context.getBean("gatewayWithPort");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(1234, accessor.getPropertyValue("registryPort"));
	}

	@Test
	public void testAdapterWithRemoteInvocationExecutorReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiGatewayParserTests.xml", this.getClass());
		RmiGateway gateway = (RmiGateway) context.getBean("gatewayWithExecutorRef");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object remoteInvocationExecutor = accessor.getPropertyValue("remoteInvocationExecutor");
		assertNotNull(remoteInvocationExecutor);
		assertEquals(StubRemoteInvocationExecutor.class, remoteInvocationExecutor.getClass());
	}

}
