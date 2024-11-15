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

package org.springframework.integration.aggregator.integration;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class AggregatorExpressionIntegrationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Test
	public void testVanillaAggregation() {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			this.input.send(new GenericMessage<>(i, headers));
		}
		assertThat(this.output.receive().getPayload()).isEqualTo("[0, 1, 2, 3, 4]");
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correllationId) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correllationId);
		return headers;
	}

}
