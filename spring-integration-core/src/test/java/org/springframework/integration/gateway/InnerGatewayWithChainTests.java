/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class InnerGatewayWithChainTests {

	@Autowired
	private TestGateway testGatewayWithErrorChannelA;

	@Autowired
	private TestGateway testGatewayWithErrorChannelAA;

	@Autowired
	private TestGateway testGatewayWithNoErrorChannelAAA;

	@Autowired
	private SourcePollingChannelAdapter inboundAdapterDefaultErrorChannel;

	@Autowired
	private SourcePollingChannelAdapter inboundAdapterAssignedErrorChannel;

	@Autowired
	private SubscribableChannel errorChannel;

	@Autowired
	private SubscribableChannel assignedErrorChannel;

	@Test
	public void testExceptionHandledByMainGateway() {
		String reply = testGatewayWithErrorChannelA.echo(5);
		assertThat(reply).isEqualTo("ERROR from errorChannelA");
	}

	@Test
	public void testExceptionHandledByMainGatewayNoErrorChannelInChain() {
		String reply = testGatewayWithErrorChannelAA.echo(0);
		assertThat(reply).isEqualTo("ERROR from errorChannelA");
	}

	@Test
	public void testExceptionHandledByInnerGateway() {
		String reply = testGatewayWithErrorChannelA.echo(0);
		assertThat(reply).isEqualTo("ERROR from errorChannelB");
	}

	// If no error channels explicitly defined exception is rethrown
	@Test
	public void testGatewaysNoErrorChannel() {
		assertThatExceptionOfType(ArithmeticException.class)
				.isThrownBy(() -> testGatewayWithNoErrorChannelAAA.echo(0));
	}

	@Test
	public void testWithSPCADefaultErrorChannel() throws Exception {
		CountDownLatch errorLatch = new CountDownLatch(1);
		errorChannel.subscribe(message -> errorLatch.countDown());
		inboundAdapterDefaultErrorChannel.start();
		assertThat(errorLatch.await(10, TimeUnit.SECONDS)).isTrue();
		inboundAdapterDefaultErrorChannel.stop();
	}

	@Test
	public void testWithSPCAAssignedErrorChannel() throws Exception {
		CountDownLatch errorLatch = new CountDownLatch(1);
		assignedErrorChannel.subscribe(message -> errorLatch.countDown());
		inboundAdapterAssignedErrorChannel.start();
		assertThat(errorLatch.await(10, TimeUnit.SECONDS)).isTrue();
		inboundAdapterAssignedErrorChannel.stop();
	}

	public interface TestGateway {

		String echo(int value);

	}

}
