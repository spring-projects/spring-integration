/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
class HeaderEnricherParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void sendTimeoutDefault() {
		Object endpoint = context.getBean("headerEnricherWithDefaults");
		long sendTimeout = TestUtils.getPropertyValue(endpoint, "handler.messagingTemplate.sendTimeout");
		assertThat(sendTimeout).isEqualTo(45000L);
	}

	@Test
	void sendTimeoutConfigured() {
		Object endpoint = context.getBean("headerEnricherWithSendTimeout");
		long sendTimeout = TestUtils.getPropertyValue(endpoint, "handler.messagingTemplate.sendTimeout");
		assertThat(sendTimeout).isEqualTo(1234L);
	}

	@Test
	void shouldSkipNullsDefault() {
		Object endpoint = context.getBean("headerEnricherWithDefaults");
		Boolean shouldSkipNulls = TestUtils.getPropertyValue(endpoint, "handler.transformer.shouldSkipNulls");
		assertThat(shouldSkipNulls).isEqualTo(Boolean.TRUE);
	}

	@Test
	void shouldSkipNullsFalseConfigured() {
		Object endpoint = context.getBean("headerEnricherWithShouldSkipNullsFalse");
		Boolean shouldSkipNulls = TestUtils.getPropertyValue(endpoint, "handler.transformer.shouldSkipNulls");
		assertThat(shouldSkipNulls).isEqualTo(Boolean.FALSE);
	}

	@Test
	void shouldSkipNullsTrueConfigured() {
		Object endpoint = context.getBean("headerEnricherWithShouldSkipNullsTrue");
		Boolean shouldSkipNulls = TestUtils.getPropertyValue(endpoint, "handler.transformer.shouldSkipNulls");
		assertThat(shouldSkipNulls).isEqualTo(Boolean.TRUE);
	}

	@Test
	void testStringPriorityHeader() {
		MessageHandler messageHandler =
				TestUtils.getPropertyValue(this.context.getBean("headerEnricherWithPriorityAsString"), "handler");
		Message<?> message = new GenericMessage<>("hello");
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> messageHandler.handleMessage(message))
				.withMessageContaining(
						"; defined in: 'class path resource " +
								"[org/springframework/integration/config/xml/HeaderEnricherParserTests-context.xml]'");
	}

	@Test
	void testStringPriorityHeaderWithType() {
		MessageHandler messageHandler =
				TestUtils.getPropertyValue(context.getBean("headerEnricherWithPriorityAsStringAndType"), "handler");
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();
		messageHandler.handleMessage(message);
		Message<?> transformed = replyChannel.receive(1000);
		assertThat(transformed).isNotNull();
		Object priority = transformed.getHeaders().get("priority");
		assertThat(priority).isNotNull();
		assertThat(priority instanceof Integer).isTrue();
	}

}
