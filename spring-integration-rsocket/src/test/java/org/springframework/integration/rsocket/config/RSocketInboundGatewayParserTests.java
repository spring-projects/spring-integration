/*
 * Copyright 2019-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.RSocketInteractionModel;
import org.springframework.integration.rsocket.inbound.RSocketInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
