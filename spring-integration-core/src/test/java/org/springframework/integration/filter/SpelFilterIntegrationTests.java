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
public class SpelFilterIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private PollableChannel positives;

	@Autowired
	private PollableChannel negatives;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired
	private PollableChannel evens;

	@Autowired
	private PollableChannel odds;

	@Test
	public void simpleExpressionBasedFilter() {
		this.simpleInput.send(new GenericMessage<>(1));
		this.simpleInput.send(new GenericMessage<>(0));
		this.simpleInput.send(new GenericMessage<>(99));
		this.simpleInput.send(new GenericMessage<>(-99));
		assertThat(positives.receive(0).getPayload()).isEqualTo(1);
		assertThat(positives.receive(0).getPayload()).isEqualTo(99);
		assertThat(negatives.receive(0).getPayload()).isEqualTo(0);
		assertThat(negatives.receive(0).getPayload()).isEqualTo(-99);
		assertThat(positives.receive(0)).isNull();
		assertThat(negatives.receive(0)).isNull();
	}

	@Test
	public void beanResolvingExpressionBasedFilter() {
		this.beanResolvingInput.send(new GenericMessage<>(1));
		this.beanResolvingInput.send(new GenericMessage<>(2));
		this.beanResolvingInput.send(new GenericMessage<>(9));
		this.beanResolvingInput.send(new GenericMessage<>(22));
		assertThat(odds.receive(0).getPayload()).isEqualTo(1);
		assertThat(odds.receive(0).getPayload()).isEqualTo(9);
		assertThat(evens.receive(0).getPayload()).isEqualTo(2);
		assertThat(evens.receive(0).getPayload()).isEqualTo(22);
		assertThat(odds.receive(0)).isNull();
		assertThat(evens.receive(0)).isNull();
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		public boolean isEven(int number) {
			return number % 2 == 0;
		}

	}

}
