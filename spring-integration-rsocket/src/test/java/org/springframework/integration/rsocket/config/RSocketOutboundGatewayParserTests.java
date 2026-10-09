/*
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket.config;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
class RSocketOutboundGatewayParserTests {

	@Autowired
	private ClientRSocketConnector clientRSocketConnector;

	@Autowired
	private RSocketOutboundGateway outboundGateway;

	@Test
	void testOutboundGatewayParser() {
		assertThat(TestUtils.<Object>getPropertyValue(this.outboundGateway, "clientRSocketConnector"))
				.isSameAs(this.clientRSocketConnector);
		assertThat(TestUtils.<String>getPropertyValue(this.outboundGateway, "interactionModelExpression.literalValue"))
				.isEqualTo("fireAndForget");
		assertThat(TestUtils.<String>getPropertyValue(this.outboundGateway, "routeExpression.expression"))
				.isEqualTo("'testRoute'");
		assertThat(TestUtils.<String>getPropertyValue(this.outboundGateway, "publisherElementTypeExpression.literalValue"))
				.isEqualTo("byte[]");
		assertThat(TestUtils.<String>getPropertyValue(this.outboundGateway, "expectedResponseTypeExpression.literalValue"))
				.isEqualTo("java.util.Date");
		Expression metadataExpression =
				TestUtils.<Expression>getPropertyValue(this.outboundGateway, "metadataExpression");
		assertThat(metadataExpression.getValue())
				.isEqualTo(Collections.singletonMap("metadata", new MimeType("*")));
	}

}
