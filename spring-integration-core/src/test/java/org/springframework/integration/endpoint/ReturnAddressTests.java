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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ReturnAddressTests {

	@Test
	public void testReturnAddressOverrides() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressOverrides.xml", this.getClass());
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		context.start();
		StringMessage message = new StringMessage("*");
		message.getHeader().setReturnAddress("replyChannel");
		channel1.send(message);
		Message<?> response = replyChannel.receive(1000);
		assertNotNull(response);
		assertEquals("**", response.getPayload());
	}

	@Test
	public void testReturnAddressIsFallbackByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressIsFallbackByDefault.xml", this.getClass());
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		context.start();
		StringMessage message = new StringMessage("*");
		message.getHeader().setReturnAddress("replyChannel");
		channel1.send(message);
		Message<?> response = replyChannel.receive(1000);
		assertNotNull(response);
		assertEquals("********", response.getPayload());
	}

}
