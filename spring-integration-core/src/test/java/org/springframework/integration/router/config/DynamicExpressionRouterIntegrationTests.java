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

package org.springframework.integration.router.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicExpressionRouterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel even;

	@Autowired
	private PollableChannel odd;

	@Test
	public void dynamicExpressionBasedRouter() {
		TestBean testBean1 = new TestBean(1);
		TestBean testBean2 = new TestBean(2);
		TestBean testBean3 = new TestBean(3);
		TestBean testBean4 = new TestBean(4);
		Message<?> message1 = MessageBuilder.withPayload(testBean1).build();
		Message<?> message2 = MessageBuilder.withPayload(testBean2).build();
		Message<?> message3 = MessageBuilder.withPayload(testBean3).build();
		Message<?> message4 = MessageBuilder.withPayload(testBean4).build();
		this.input.send(message1);
		this.input.send(message2);
		this.input.send(message3);
		this.input.send(message4);
		assertThat(odd.receive(0).getPayload()).isEqualTo(testBean1);
		assertThat(even.receive(0).getPayload()).isEqualTo(testBean2);
		assertThat(odd.receive(0).getPayload()).isEqualTo(testBean3);
		assertThat(even.receive(0).getPayload()).isEqualTo(testBean4);
		assertThat(odd.receive(0)).isNull();
		assertThat(even.receive(0)).isNull();
	}

	static class TestBean {

		private final int number;

		TestBean(int number) {
			this.number = number;
		}

		public int getNumber() {
			return this.number;
		}

	}

}
