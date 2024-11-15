/*
 * Copyright 2002-2024 the original author or authors.
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
