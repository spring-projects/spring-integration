/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelSplitterIntegrationTests {

	@Autowired @Qualifier("input")
	private MessageChannel input;

	@Autowired @Qualifier("output")
	private PollableChannel output;


	@Test
	public void split() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).build();
		this.input.send(message);
		assertEquals(new Integer(1), output.receive(0).getPayload());
		assertEquals(new Integer(2), output.receive(0).getPayload());
		assertEquals(new Integer(3), output.receive(0).getPayload());
		assertEquals(new Integer(4), output.receive(0).getPayload());
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
	}

}
