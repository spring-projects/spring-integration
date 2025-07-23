/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.amqp.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class OutboundGatewayIntegrationTests implements RabbitTestContainer {

	@Autowired
	private MessageChannel toRabbit;

	@Autowired
	private PollableChannel fromRabbit;

	@Test
	public void testOutboundInboundGateways() throws Exception {
		String payload = "foo";
		this.toRabbit.send(new GenericMessage<String>(payload));
		Message<?> receive = this.fromRabbit.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(payload.toUpperCase());
	}

	public static class EchoBean {

		String echo(String o) {
			return o.toUpperCase();
		}

	}

}
