/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.httpinvoker.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.httpinvoker.HttpInvokerInboundGateway;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("deprecation")
public class HttpInvokerInboundGatewayParserTests {

	@Test
	public void gatewayWithDefaults() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerInboundGatewayParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		HttpInvokerInboundGateway gateway = (HttpInvokerInboundGateway) context.getBean("gatewayWithDefaults");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(true, accessor.getPropertyValue("expectReply"));
		assertEquals(channel, accessor.getPropertyValue("requestChannel"));
		MessagingTemplate template = (MessagingTemplate)
				accessor.getPropertyValue("messagingTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(-1L, templateAccessor.getPropertyValue("sendTimeout"));
		assertEquals(-1L, templateAccessor.getPropertyValue("receiveTimeout"));
	}

	@Test
	public void gatewayWithName() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerInboundGatewayParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		HttpInvokerInboundGateway gateway = (HttpInvokerInboundGateway) context.getBean("/gateway/with/name");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(true, accessor.getPropertyValue("expectReply"));
		assertEquals(channel, accessor.getPropertyValue("requestChannel"));
		MessagingTemplate template = (MessagingTemplate)
				accessor.getPropertyValue("messagingTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(-1L, templateAccessor.getPropertyValue("sendTimeout"));
		assertEquals(-1L, templateAccessor.getPropertyValue("receiveTimeout"));
	}

	@Test
	public void gatewayWithCustomProperties() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerInboundGatewayParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		HttpInvokerInboundGateway gateway = (HttpInvokerInboundGateway) context.getBean("gatewayWithCustomProperties");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(false, accessor.getPropertyValue("expectReply"));
		assertEquals(channel, accessor.getPropertyValue("requestChannel"));
		MessagingTemplate template = (MessagingTemplate)
				accessor.getPropertyValue("messagingTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(123L, templateAccessor.getPropertyValue("sendTimeout"));
		assertEquals(456L, templateAccessor.getPropertyValue("receiveTimeout"));
	}

}
