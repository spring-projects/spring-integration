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

package org.springframework.integration.router.config;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class SplitterParserTests {

	@Test
	public void splitterAdapterWithRefAndMethod() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterAdapterWithRefAndMethodInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> result1 = output.receive(1000);
		assertEquals("this", result1.getPayload());
		Message<?> result2 = output.receive(1000);
		assertEquals("is", result2.getPayload());
		Message<?> result3 = output.receive(1000);
		assertEquals("a", result3.getPayload());
		Message<?> result4 = output.receive(1000);
		assertEquals("test", result4.getPayload());
		assertNull(output.receive(0));
	}

	@Test
	public void splitterAdapterWithRefOnly() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterAdapterWithRefOnlyInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> result1 = output.receive(1000);
		assertEquals("this", result1.getPayload());
		Message<?> result2 = output.receive(1000);
		assertEquals("is", result2.getPayload());
		Message<?> result3 = output.receive(1000);
		assertEquals("a", result3.getPayload());
		Message<?> result4 = output.receive(1000);
		assertEquals("test", result4.getPayload());
		assertNull(output.receive(0));
	}

	@Test
	public void splitterImplementation() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterImplementationInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("this.is.a.test"));
		Message<?> result1 = output.receive(1000);
		assertEquals("this", result1.getPayload());
		Message<?> result2 = output.receive(1000);
		assertEquals("is", result2.getPayload());
		Message<?> result3 = output.receive(1000);
		assertEquals("a", result3.getPayload());
		Message<?> result4 = output.receive(1000);
		assertEquals("test", result4.getPayload());
		assertNull(output.receive(0));
	}

	@Test(expected = ReplyRequiredException.class)
	public void splitterParserTestWithRequiresReply() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		DirectChannel inputChannel = context.getBean("requiresReplyInput", DirectChannel.class);
		inputChannel.send(MessageBuilder.withPayload(Collections.emptyList()).build());
	}

	@Test
	public void splitterParserTestApplySequenceFalse() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		DirectChannel inputChannel = context.getBean("noSequenceInput", DirectChannel.class);
		PollableChannel output = (PollableChannel) context.getBean("output");
		inputChannel.send(MessageBuilder.withPayload(Collections.emptyList()).build());
		Message<?> message = output.receive(1000);
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceNumber(), is(0));
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize(), is(0));
	}


}
