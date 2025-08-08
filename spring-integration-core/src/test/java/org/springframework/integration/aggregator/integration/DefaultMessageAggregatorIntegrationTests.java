/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator.integration;

import java.util.Arrays;

import org.assertj.core.api.InstanceOfAssertFactories;
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
		assertThat(payload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
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
