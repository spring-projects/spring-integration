/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.splitter;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
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
		assertThat(one.getPayload()).isEqualTo(1);
		assertThat(one.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(two.getPayload()).isEqualTo(2);
		assertThat(two.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(three.getPayload()).isEqualTo(3);
		assertThat(three.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(four.getPayload()).isEqualTo(4);
		assertThat(four.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(output.receive(0)).isNull();
	}

	static class TestBean {

		private final List<Integer> numbers = new ArrayList<>();

		TestBean() {
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
