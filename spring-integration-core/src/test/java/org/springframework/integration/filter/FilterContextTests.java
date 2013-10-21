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

package org.springframework.integration.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class FilterContextTests {

	@Test
	public void methodInvokingFilterRejects() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"filterContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("foo"));
		Message<?> reply = output.receive(0);
		assertNull(reply);
	}

	@Test
	public void methodInvokingFilterAccepts() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"filterContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("foobar"));
		Message<?> reply = output.receive(0);
		assertEquals("foobar", reply.getPayload());
	}

}
