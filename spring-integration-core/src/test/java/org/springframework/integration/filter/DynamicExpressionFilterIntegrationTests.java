/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class DynamicExpressionFilterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel positives;

	@Autowired
	private PollableChannel negatives;

	@Test
	void simpleExpressionBasedFilter() {
		this.input.send(new GenericMessage<>(1));
		this.input.send(new GenericMessage<>(0));
		this.input.send(new GenericMessage<>(99));
		this.input.send(new GenericMessage<>(-99));
		assertThat(positives.receive(0).getPayload()).isEqualTo(1);
		assertThat(positives.receive(0).getPayload()).isEqualTo(99);
		assertThat(negatives.receive(0).getPayload()).isEqualTo(0);
		assertThat(negatives.receive(0).getPayload()).isEqualTo(-99);
		assertThat(positives.receive(0)).isNull();
		assertThat(negatives.receive(0)).isNull();
	}

}
