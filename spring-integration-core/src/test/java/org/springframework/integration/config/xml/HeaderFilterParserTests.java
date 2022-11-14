/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class HeaderFilterParserTests {

	@Autowired
	private MessageChannel inputA;

	@Autowired
	private MessageChannel inputB;

	@Autowired
	private MessageChannel inputC;

	@Autowired
	private MessageChannel inputD;

	@Autowired
	private MessageChannel inputE;

	@Test
	public void verifyHeadersRemoved() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("a", 1)
				.setHeader("b", 2)
				.setHeader("c", 3)
				.setHeader("d", 4)
				.setHeader("e", 5)
				.build();
		inputA.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("a")).isNull();
		assertThat(result.getHeaders().get("c")).isNull();
		assertThat(result.getHeaders().get("d")).isNull();
		assertThat(result.getHeaders().get("b")).isNotNull();
		assertThat(result.getHeaders().get("e")).isNotNull();
	}

	@Test
	public void verifyHeadersRemovedWithSingleWildcard() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("a", 1)
				.setHeader("b", 2)
				.setHeader("c", 3)
				.setHeader("d", 4)
				.setHeader("e", 5)
				.build();
		inputB.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("a")).isNull();
		assertThat(result.getHeaders().get("c")).isNull();
		assertThat(result.getHeaders().get("d")).isNull();
		assertThat(result.getHeaders().get("b")).isNull();
		assertThat(result.getHeaders().get("e")).isNull();
	}

	@Test
	public void verifyHeadersRemovedWithNamePatterns() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("bar", 1)
				.setHeader("baz", 2)
				.setHeader("foo", 3)
				.setHeader("goo", 4)
				.setHeader("e", 5)
				.build();
		inputC.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("bar")).isNull();
		assertThat(result.getHeaders().get("baz")).isNull();
		assertThat(result.getHeaders().get("foo")).isNull();
		assertThat(result.getHeaders().get("goo")).isNull();
		assertThat(result.getHeaders().get("e")).isNotNull();
	}

	@Test
	public void verifyHeadersRemovedWhereHeaderNameContainsWildCard() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("bar*", 1)
				.setHeader("bart", 2)
				.setHeader("foo", 3)
				.setHeader("goo", 4)
				.setHeader("e", 5)
				.build();
		inputD.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("bar*")).isNull();
		assertThat(result.getHeaders().get("bart")).isNull();
		assertThat(result.getHeaders().get("foo")).isNull();
		assertThat(result.getHeaders().get("goo")).isNull();
		assertThat(result.getHeaders().get("e")).isNotNull();
	}

	@Test
	public void verifyHeadersRemovedWhereHeaderNameIsLiteral() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("bar*", 1)
				.setHeader("bart", 2)
				.setHeader("foo", 3)
				.setHeader("goo", 4)
				.setHeader("e", 5)
				.build();
		inputE.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("bar*")).isNull();
		assertThat(result.getHeaders().get("bart")).isNotNull();
		assertThat(result.getHeaders().get("foo")).isNull();
		assertThat(result.getHeaders().get("goo")).isNotNull();
		assertThat(result.getHeaders().get("e")).isNotNull();
	}

}
