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

package org.springframework.integration.adapter.httpinvoker.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.httpinvoker.HttpInvokerSourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.RequestReplyTemplate;

/**
 * @author Mark Fisher
 */
public class HttpInvokerSourceAdapterParserTests {

	@Test
	public void testAdapterWithDefaults() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerSourceAdapterParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		HttpInvokerSourceAdapter adapter = (HttpInvokerSourceAdapter) context.getBean("adapterWithDefaults");
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		assertEquals(channel, accessor.getPropertyValue("requestChannel"));
		assertEquals(true, accessor.getPropertyValue("expectReply"));
		RequestReplyTemplate template = (RequestReplyTemplate)
				accessor.getPropertyValue("requestReplyTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(-1L, templateAccessor.getPropertyValue("requestTimeout"));
		assertEquals(-1L, templateAccessor.getPropertyValue("replyTimeout"));
	}

	@Test
	public void testAdapterWithName() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerSourceAdapterParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		HttpInvokerSourceAdapter adapter = (HttpInvokerSourceAdapter) context.getBean("/adapter/with/name");
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		assertEquals(channel, accessor.getPropertyValue("requestChannel"));
		assertEquals(true, accessor.getPropertyValue("expectReply"));
		RequestReplyTemplate template = (RequestReplyTemplate)
				accessor.getPropertyValue("requestReplyTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(-1L, templateAccessor.getPropertyValue("requestTimeout"));
		assertEquals(-1L, templateAccessor.getPropertyValue("replyTimeout"));
	}

	@Test
	public void testAdapterWithCustomProperties() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"httpInvokerSourceAdapterParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		HttpInvokerSourceAdapter adapter = (HttpInvokerSourceAdapter) context.getBean("adapterWithCustomProperties");
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		assertEquals(channel, accessor.getPropertyValue("requestChannel"));
		assertEquals(false, accessor.getPropertyValue("expectReply"));
		RequestReplyTemplate template = (RequestReplyTemplate)
				accessor.getPropertyValue("requestReplyTemplate");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(template);
		assertEquals(123L, templateAccessor.getPropertyValue("requestTimeout"));
		assertEquals(456L, templateAccessor.getPropertyValue("replyTimeout"));
	}

}
