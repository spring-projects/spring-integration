/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class DefaultMessageAggregatorIntegrationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Test
	public void testAggregation() {
		for (int i = 0; i < 5; i++) {
			this.input.send(prepareSequenceMessage(i, 5, 1));
		}
		Object payload = this.output.receive(20_000).getPayload();
		assertThat(payload).isInstanceOf(List.class)
				.asList()
				.containsAll(Arrays.asList(0, 1, 2, 3, 4));
	}


	private static Message<?> prepareSequenceMessage(int sequenceNumber, int sequenceSize, int correlationId) {
		return MessageBuilder.withPayload(sequenceNumber)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.setCorrelationId(correlationId)
				.build();
	}

}
