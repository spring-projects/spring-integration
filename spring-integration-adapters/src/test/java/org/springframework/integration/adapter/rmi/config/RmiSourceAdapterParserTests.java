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

package org.springframework.integration.adapter.rmi.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.rmi.RmiSourceAdapter;
import org.springframework.integration.channel.MessageChannel;

/**
 * @author Mark Fisher
 */
public class RmiSourceAdapterParserTests {

	@Test
	public void testAdapterWithDefaults() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiSourceAdapterParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		RmiSourceAdapter adapter = (RmiSourceAdapter) context.getBean("adapterWithDefaults");
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		assertEquals(channel, accessor.getPropertyValue("channel"));
		assertEquals(true, accessor.getPropertyValue("expectReply"));
		assertEquals(-1L, accessor.getPropertyValue("sendTimeout"));
		assertEquals(-1L, accessor.getPropertyValue("receiveTimeout"));
	}

	@Test
	public void testAdapterWithCustomProperties() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiSourceAdapterParserTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		RmiSourceAdapter adapter = (RmiSourceAdapter) context.getBean("adapterWithCustomProperties");
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		assertEquals(channel, accessor.getPropertyValue("channel"));
		assertEquals(false, accessor.getPropertyValue("expectReply"));
		assertEquals(123L, accessor.getPropertyValue("sendTimeout"));
		assertEquals(456L, accessor.getPropertyValue("receiveTimeout"));
	}

}
