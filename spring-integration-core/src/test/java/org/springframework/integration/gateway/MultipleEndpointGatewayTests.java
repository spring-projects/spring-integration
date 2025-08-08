/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class MultipleEndpointGatewayTests {

	@Autowired
	@Qualifier("gatewayA")
	private SampleGateway gatewayA;

	@Autowired
	@Qualifier("gatewayB")
	private SampleGateway gatewayB;

	@Test
	public void gatewayNoDefaultReplyChannel() {
		Assertions.assertThatNoException().isThrownBy(() -> gatewayA.echo("echoAsMessageChannel"));
	}

	@Test
	public void gatewayWithDefaultReplyChannel() {
		Assertions.assertThatNoException().isThrownBy(() -> gatewayB.echo("echoAsMessageChannelIgnoreDefOutChannel"));
	}

	@Test
	public void gatewayWithReplySentBackToDefaultReplyChannel() {
		Assertions.assertThatNoException().isThrownBy(() -> gatewayB.echo("echoAsMessageChannelDefaultOutputChannel"));
	}

	public interface SampleGateway {

		Object echo(Object value);

	}

	public static class SampleEchoService {

		public Object echo(Object value) {
			return "R:" + value;
		}

		public Message<?> echoAsMessage(Object value) {
			return MessageBuilder.withPayload("R:" + value).build();
		}

	}

}
