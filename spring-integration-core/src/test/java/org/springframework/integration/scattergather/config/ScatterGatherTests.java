/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.scattergather.config;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class ScatterGatherTests {

	@Autowired
	private PollableChannel output;

	@Autowired
	private MessageChannel inputAuction;

	@Autowired
	private MessageChannel inputDistribution;

	@Autowired
	private RequestReplyExchanger gateway;

	@Autowired
	private MessageChannel scatterGatherWithinChain;

	@Test
	public void testAuction() {
		this.inputAuction.send(new GenericMessage<>("foo"));
		Message<?> bestQuoteMessage = this.output.receive(10000);
		assertThat(bestQuoteMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	public void testDistribution() {
		this.inputDistribution.send(new GenericMessage<>("foo"));
		Message<?> bestQuoteMessage = this.output.receive(10000);
		assertThat(bestQuoteMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	public void testGatewayScatterGather() {
		Message<?> bestQuoteMessage = this.gateway.exchange(new GenericMessage<>("foo"));
		assertThat(bestQuoteMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSizeGreaterThanOrEqualTo(1);

		bestQuoteMessage = this.gateway.exchange(new GenericMessage<>("bar"));
		assertThat(bestQuoteMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	public void testWithinChain() {
		this.scatterGatherWithinChain.send(new GenericMessage<>("foo"));
		for (int i = 0; i < 3; i++) {
			Message<?> result = this.output.receive(10000);
			assertThat(result).isNotNull();
		}
	}

}
