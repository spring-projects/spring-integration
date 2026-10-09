/*
 * Copyright 2002-present the original author or authors.
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
