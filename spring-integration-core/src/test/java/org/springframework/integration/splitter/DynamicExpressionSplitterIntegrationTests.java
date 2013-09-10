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

package org.springframework.integration.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicExpressionSplitterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;


	@Test
	public void simple() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).setHeader("foo", "foo").build();
		this.input.send(message);
		Message<?> one = output.receive(0);
		Message<?> two = output.receive(0);
		Message<?> three = output.receive(0);
		Message<?> four = output.receive(0);
		assertEquals(new Integer(1), one.getPayload());
		assertEquals("foo", one.getHeaders().get("foo"));
		assertEquals(new Integer(2), two.getPayload());
		assertEquals("foo", two.getHeaders().get("foo"));
		assertEquals(new Integer(3), three.getPayload());
		assertEquals("foo", three.getHeaders().get("foo"));
		assertEquals(new Integer(4), four.getPayload());
		assertEquals("foo", four.getHeaders().get("foo"));
		assertNull(output.receive(0));
	}


	static class TestBean {

		private final List<Integer> numbers = new ArrayList<Integer>();

		public TestBean() {
			for (int i = 1; i <= 10; i++) {
				this.numbers.add(i);
			}
		}

		public List<Integer> getNumbers() {
			return this.numbers;
		}

		public String[] split(String s) {
			return s.split(",");
		}
	}

}
