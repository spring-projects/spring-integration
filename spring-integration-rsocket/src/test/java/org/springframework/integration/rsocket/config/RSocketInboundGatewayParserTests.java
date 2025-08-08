/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.RSocketInteractionModel;
import org.springframework.integration.rsocket.inbound.RSocketInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
class RSocketInboundGatewayParserTests {

	@Autowired
	private ClientRSocketConnector clientRSocketConnector;

	@Autowired
	private RSocketInboundGateway inboundGateway;

	@Test
	void testInboundGatewayParser() {
		assertThat(TestUtils.getPropertyValue(this.inboundGateway, "rsocketConnector"))
				.isSameAs(this.clientRSocketConnector);
		assertThat(TestUtils.getPropertyValue(this.inboundGateway, "rsocketStrategies"))
				.isSameAs(this.clientRSocketConnector.getRSocketStrategies());
		assertThat(this.inboundGateway.getPath()).containsExactly("testPath");
		assertThat(TestUtils.getPropertyValue(this.inboundGateway, "requestElementType.resolved"))
				.isEqualTo(byte[].class);
		assertThat(this.inboundGateway.getInteractionModels())
				.containsExactly(RSocketInteractionModel.fireAndForget, RSocketInteractionModel.requestChannel);
		assertThat(TestUtils.getPropertyValue(this.inboundGateway, "decodeFluxAsUnit", Boolean.class)).isTrue();
	}

}
