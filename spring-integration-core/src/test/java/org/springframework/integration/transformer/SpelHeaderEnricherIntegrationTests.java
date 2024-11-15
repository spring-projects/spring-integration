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

package org.springframework.integration.transformer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class SpelHeaderEnricherIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired
	private MessageChannel expressionNotExecutedInput;

	@Autowired
	private MessageChannel headerNotRemovedInput;

	@Autowired
	private MessageChannel headerRemovedInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Test
	public void simple() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.simpleInput.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getHeaders().get("testHeader")).isEqualTo(8);
	}

	@Test
	public void beanResolving() {
		Message<?> message = MessageBuilder.withPayload("test").setHeader("num", 3).build();
		this.beanResolvingInput.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getHeaders().get("num")).isEqualTo(243);
	}

	@Test
	public void suppressBeanExecution() {
		Message<?> message = MessageBuilder.withPayload("test").setHeader("num", 3).build();
		this.expressionNotExecutedInput.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getHeaders().get("num")).isEqualTo(3);
	}

	@Test
	public void headerNotRemoved() {
		Message<?> message = MessageBuilder.withPayload("test").setHeader("num", 3).build();
		this.headerNotRemovedInput.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getHeaders().get("num")).isEqualTo(3);
	}

	@Test
	public void headerRemoved() {
		Message<?> message = MessageBuilder.withPayload("test").setHeader("num", 3).build();
		this.headerRemovedInput.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getHeaders().get("num")).isNull();
	}

	static class TestBean {

		public int getValue() {
			return 5;
		}

	}

}
