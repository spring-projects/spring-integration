/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import java.io.IOException;

import io.nats.client.Connection;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test negative scenarios - Publishing messages to NATS server without Jetstream
 *
 * <p>Manual test cases to test NATS spring components communication with NATS server.
 *
 * <p>Prerequisite: set below properties and then start the test. -Dnats_js_enabled=false
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {NatsTestConfig.class})
public class NatsOutboundAdapterNegativeTest extends AbstractNatsIntegrationTestSupport {

	@Autowired
	private Connection natsConnection;

	/**
	 * Method tests the behavior of the application when NATS server is not configured with jetstream.
	 *
	 * <p>Test scenario:
	 *
	 * <p>1.Start NATS server without JetSTREAM enabled
	 *
	 * <p>2.Result: send messages to jetstream topic check exceptions thrown and error messages
	 */
	@Test
	public void testMessageSendingHandlerOnNoJetStreamFound() {
		// Configuration of template and message producer components
		final MessageConverter<String> messageConverter = new MessageConverter<>(String.class);
		final NatsTemplate natsTemplate =
				new NatsTemplate(this.natsConnection, "validSubject", messageConverter);
		final NatsMessageProducingHandler ipgNatsMessageProducingHandler =
				new NatsMessageProducingHandler(natsTemplate);
		final MessageDeliveryException messageDeliveryException =
				assertThrows(
						MessageDeliveryException.class,
						() ->
								ipgNatsMessageProducingHandler.handleMessageInternal(
										MessageBuilder.withPayload("testing").build()));
		assertEquals(IOException.class, messageDeliveryException.getCause().getClass());
		assertTrue(
				messageDeliveryException
						.getMessage()
						.contains("Exception occurred while sending message to invalid_subject"));
	}
}
