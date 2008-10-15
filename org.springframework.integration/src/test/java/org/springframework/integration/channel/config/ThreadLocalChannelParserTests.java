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

package org.springframework.integration.channel.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.ThreadLocalChannel;
import org.springframework.integration.core.MessageChannel;

/**
 * @author Mark Fisher
 */
public class ThreadLocalChannelParserTests {

	@Test
	public void testChannelType() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"threadLocalChannelParserTests.xml", ThreadLocalChannelParserTests.class);
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		assertEquals(ThreadLocalChannel.class, channel.getClass());
	}

}
