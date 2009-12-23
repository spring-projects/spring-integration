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

package org.springframework.integration.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.selector.MessageSelector;

/**
 * @author Mark Fisher
 * @author Grzegorz Grzybek
 */
public class FilterContextTests {

	@Test
	public void methodInvokingFilterRejects() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"filterContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new StringMessage("foo"));
		Message<?> reply = output.receive(0);
		assertNull(reply);
	}

	@Test
	public void methodInvokingFilterAccepts() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"filterContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new StringMessage("foobar"));
		Message<?> reply = output.receive(0);
		assertEquals("foobar", reply.getPayload());
	}

	@Test
	public void filterWithEmbeddedSelector() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"filterContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input-embedded-selector");
		PollableChannel output = (PollableChannel) context.getBean("output-embedded-selector");
		input.send(new StringMessage("foo"));
		Message<?> reply = output.receive(0);
		assertNull(reply);
	}

	@Test
	public void filterWithEmbeddedBean() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"filterContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input-embedded");
		PollableChannel output = (PollableChannel) context.getBean("output-embedded");
		input.send(new StringMessage("foo"));
		Message<?> reply = output.receive(0);
		assertNull(reply);
	}


	static class TestSelector implements MessageSelector {

		public boolean accept(Message<?> message) {
			return false;
		}
	}

}
