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

package org.springframework.integration.aggregator.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author David Liu
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class ResequencerIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void validateUnboundedResequencerLight() {
		MessageChannel inputChannel = context.getBean("resequencerLightInput", MessageChannel.class);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		EventDrivenConsumer edc = context.getBean("resequencerLight", EventDrivenConsumer.class);
		ResequencingMessageHandler handler = TestUtils.getPropertyValue(edc, "handler");
		MessageGroupStore store = TestUtils.getPropertyValue(handler, "messageStore");

		Message<?> message1 = MessageBuilder.withPayload("1").setCorrelationId("A").setSequenceNumber(1).build();
		Message<?> message2 = MessageBuilder.withPayload("2").setCorrelationId("A").setSequenceNumber(2)
				.setCorrelationId("A")
				.setSequenceNumber(2)
				.setHeader("foo", "foo")
				.build();
		Message<?> message3 = MessageBuilder.withPayload("3").setCorrelationId("A").setSequenceNumber(3).build();
		Message<?> message4 = MessageBuilder.withPayload("4")
				.setCorrelationId("A")
				.setSequenceNumber(4)
				.setHeader("foo", "foo")
				.build();
		Message<?> message5 = MessageBuilder.withPayload("5").setCorrelationId("A").setSequenceNumber(5).build();
		Message<?> message6 = MessageBuilder.withPayload("6").setCorrelationId("A").setSequenceNumber(6).build();

		inputChannel.send(message3);
		assertThat(outputChannel.receive(0)).isNull();

		inputChannel.send(message1);
		message1 = outputChannel.receive(0);
		assertThat(message1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(message1).getSequenceNumber()).isEqualTo(1);
		assertThat(message1.getHeaders().containsKey("foo")).isFalse();

		inputChannel.send(message2);
		message2 = outputChannel.receive(0);
		message3 = outputChannel.receive(0);
		assertThat(message2).isNotNull();
		assertThat(message3).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(message2).getSequenceNumber()).isEqualTo(2);
		assertThat(message2.getHeaders().containsKey("foo")).isTrue();
		assertThat(new IntegrationMessageHeaderAccessor(message3).getSequenceNumber()).isEqualTo(3);
		assertThat(message3.getHeaders().containsKey("foo")).isFalse();

		inputChannel.send(message5);
		assertThat(outputChannel.receive(0)).isNull();

		inputChannel.send(message6);
		assertThat(outputChannel.receive(0)).isNull();

		inputChannel.send(message4);
		message4 = outputChannel.receive(0);
		message5 = outputChannel.receive(0);
		message6 = outputChannel.receive(0);
		assertThat(message4).isNotNull();
		assertThat(message5).isNotNull();
		assertThat(message6).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(message4).getSequenceNumber()).isEqualTo(4);
		assertThat(message4.getHeaders().containsKey("foo")).isTrue();
		assertThat(new IntegrationMessageHeaderAccessor(message5).getSequenceNumber()).isEqualTo(5);
		assertThat(message5.getHeaders().containsKey("foo")).isFalse();
		assertThat(new IntegrationMessageHeaderAccessor(message6).getSequenceNumber()).isEqualTo(6);
		assertThat(message6.getHeaders().containsKey("foo")).isFalse();

		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(0);
	}

	@Test
	public void validateUnboundedResequencerDeep() {
		MessageChannel inputChannel = context.getBean("resequencerDeepInput", MessageChannel.class);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		EventDrivenConsumer edc = context.getBean("resequencerDeep", EventDrivenConsumer.class);
		ResequencingMessageHandler handler = TestUtils.getPropertyValue(edc, "handler");
		MessageGroupStore store = TestUtils.getPropertyValue(handler, "messageStore");

		Message<?> message1 = MessageBuilder.withPayload("1").setCorrelationId("A").setSequenceNumber(1).build();
		Message<?> message2 = MessageBuilder.withPayload("2").setCorrelationId("A").setSequenceNumber(2).build();
		Message<?> message3 = MessageBuilder.withPayload("3").setCorrelationId("A").setSequenceNumber(3).build();

		inputChannel.send(message3);
		assertThat(outputChannel.receive(0)).isNull();
		inputChannel.send(message1);
		assertThat(outputChannel.receive(0)).isNotNull();
		inputChannel.send(message2);
		assertThat(outputChannel.receive(0)).isNotNull();
		assertThat(outputChannel.receive(0)).isNotNull();
		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(0);
	}

	@Test
	public void testResequencerRefServiceActivator() {
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		Message<?> message1 = MessageBuilder.withPayload("1").setCorrelationId("A").setSequenceNumber(1).build();
		inputChannel.send(message1);
		message1 = outputChannel.receive(0);
		assertThat(message1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(message1).getSequenceNumber()).isEqualTo(1);
	}

}
