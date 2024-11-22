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

package org.springframework.integration.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class TransactionSynchronizationQueueChannelTests {

	@Autowired
	private QueueChannel queueChannel;

	@Autowired
	private QueueChannel good;

	@Autowired
	private QueueChannel queueChannel2;

	@BeforeEach
	public void setup() {
		this.good.purge(null);
		this.queueChannel.purge(null);
		this.queueChannel2.purge(null);
	}

	@Test
	public void testCommit() {
		GenericMessage<String> sentMessage = new GenericMessage<>("hello");
		this.queueChannel.send(sentMessage);
		Message<?> message = this.good.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("hello");
		assertThat(sentMessage).isSameAs(message);
	}

	@Test
	public void testRollback() {
		this.queueChannel.send(new GenericMessage<>("fail"));
		Message<?> message = this.good.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("retry:fail");
	}

	@Test
	public void testIncludeChannelName() {
		Message<String> sentMessage = MessageBuilder.withPayload("hello")
				.setHeader("foo", "bar").build();
		queueChannel2.send(sentMessage);
		Message<?> message = good.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("hello processed ok from queueChannel2");
		assertThat(message.getHeaders().get("foo")).isNotNull();
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	public static class Service {

		public void handle(String foo) {
			if (foo.startsWith("fail")) {
				throw new RuntimeException("planned failure");
			}
		}

	}

}
