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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class SplitterParserTests {

	@Test
	public void splitterAdapterWithRefAndMethod() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("splitterAdapterWithRefAndMethodInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new StringMessage("this.is.a.test"));
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
		input.send(new StringMessage("this.is.a.test"));
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
		input.send(new StringMessage("this.is.a.test"));
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

}
