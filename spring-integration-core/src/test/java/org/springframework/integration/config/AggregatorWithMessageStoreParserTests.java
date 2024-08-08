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

package org.springframework.integration.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AggregatorWithMessageStoreParserTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	private TestAggregatorBean aggregatorBean;

	@Autowired
	private MessageGroupStore messageGroupStore;

	@Autowired
	private MessageChannel controlBusChannel;

	@Test
	public void testAggregation() {
		input.send(createMessage("123", "id1", 3, 1, null));
		assertThat(messageGroupStore.getMessageGroup("id1").size()).isEqualTo(1);
		input.send(createMessage("789", "id1", 3, 3, null));
		assertThat(messageGroupStore.getMessageGroup("id1").size()).isEqualTo(2);
		input.send(createMessage("456", "id1", 3, 2, null));
		assertThat(aggregatorBean
				.getAggregatedMessages().size()).as("One and only one message should have been aggregated")
				.isEqualTo(1);
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		assertThat(aggregatedMessage
				.getPayload()).as("The aggregated message payload is not correct").isEqualTo("123456789");
	}

	@Test
	public void testExpiry() {
		input.send(createMessage("123", "id1", 3, 1, null));
		assertThat(messageGroupStore.getMessageGroup("id1").size()).isEqualTo(1);
		input.send(createMessage("456", "id1", 3, 2, null));
		assertThat(messageGroupStore.getMessageGroup("id1").size()).isEqualTo(2);
		this.controlBusChannel.send(
				MessageBuilder.withPayload("messageStore.expireMessageGroups")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of(-10000L))
						.build());
		assertThat(aggregatorBean
				.getAggregatedMessages().size()).as("One and only one message should have been aggregated")
				.isEqualTo(1);
		Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
		assertThat(aggregatedMessage
				.getPayload()).as("The aggregated message payload is not correct").isEqualTo("123456");
	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {

		return MessageBuilder.withPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReplyChannel(outputChannel).build();
	}

}
