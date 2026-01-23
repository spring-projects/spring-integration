/*
 * Copyright 2019-present the original author or authors.
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
