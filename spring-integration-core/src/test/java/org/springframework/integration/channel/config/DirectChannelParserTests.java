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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.dispatcher.SynchronousChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DirectChannelParserTests {

	@Test
	public void testChannelWithoutSource() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"directChannelParserTests.xml", DirectChannelParserTests.class);
		SynchronousChannel channel = (SynchronousChannel) context.getBean("channelWithoutSource");
		assertNull(channel.receive());
		Message<?> message = new StringMessage("test");
		assertTrue(channel.send(message));
		Message<?> reply = channel.receive();
		assertNotNull(reply);
		assertEquals(message, reply);
	}

	@Test
	public void testChannelWithSource() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"directChannelParserTests.xml", DirectChannelParserTests.class);
		SynchronousChannel channel = (SynchronousChannel) context.getBean("channelWithSource");
		assertFalse(channel.send(new StringMessage("test")));
		Message<?> reply = channel.receive();
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

}
