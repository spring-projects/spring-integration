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
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ReturnAddressTests {

	@Test
	public void testNextTargetOverrides() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1WithOverride");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		Message<String> message = MessageBuilder.fromPayload("*")
				.setNextTarget("replyChannel").build();
		channel1.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertNotNull(response);
		PollableChannel outputChannel = (PollableChannel) context.getBean("channel2");
		assertNull(outputChannel.receive(0));
		assertEquals("**", response.getPayload());
	}

	@Test
	public void testOutputTakesPrecedenceByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		Message<String> message = MessageBuilder.fromPayload("*")
				.setReturnAddress("replyChannel").build();
		channel1.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertNotNull(response);
		assertEquals("********", response.getPayload());
	}

	@Test
	public void testOutputTakesPrecedenceAndNoReturnAddress() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel4 = (MessageChannel) context.getBean("channel4");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		StringMessage message = new StringMessage("*");
		channel4.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertNotNull(response);
		assertEquals("**", response.getPayload());
	}

	@Test
	public void testReturnAddressFallbackButNotAvailable() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		context.start();
		StringMessage message = new StringMessage("*");
		channel3.send(message);
		Message<?> errorMessage = errorChannel.receive(3000);
		assertNotNull(errorMessage.getPayload());
	}

	@Test
	public void testOutputFallbackButNotAvailable() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3WithOverride");
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		context.start();
		StringMessage message = new StringMessage("*");
		channel3.send(message);
		Message<?> errorMessage = errorChannel.receive(3000);
		assertNotNull(errorMessage.getPayload());
	}

}
