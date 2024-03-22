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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.exception.NatsException;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test negative scenarios - Connecting NATS server without Jetstream (NATS Subscription
 * not available)
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
public class NatsMessageDrivenChannelAdapterNegativeFlowTest
		extends AbstractNatsIntegrationTestSupport {

	@Autowired
	private Connection natsConnection;

	/**
	 * Method tests the behavior of the application when NATS server is not configured with jetstream.
	 *
	 * <p>Test scenario:
	 *
	 * <p>1.Start NATS server without JetSTREAM enabled
	 *
	 * <p>2.Result: check exceptions thrown and error messages
	 */
	@Test
	public void testSubscriptionNotAvailableOnNoJetStreamFound() {
		// Configuration of adapter and container components
		final ConsumerProperties consumerProperties =
				new ConsumerProperties("validStream", "validSubject", "valid-consumer", "valid-group");
		final NatsMessageListenerContainer container =
				new NatsMessageListenerContainer(
						new NatsConsumerFactory(this.natsConnection, consumerProperties));
		final NatsMessageDrivenChannelAdapter adapter = new NatsMessageDrivenChannelAdapter(container);
		final NatsMessageDrivenChannelAdapter.NatsMessageHandler messageHandler =
				adapter.new NatsMessageHandler();
		container.setMessageHandler(messageHandler);
		// Assert that adapter and container is not running before start
		assertFalse(adapter.isRunning());
		assertFalse(container.isRunning());
		// Check if NATS exception is thrown with expected message
		final NatsException exception = assertThrows(NatsException.class, () -> container.start());
		assertEquals(IOException.class, exception.getCause().getClass());
		assertTrue(
				exception
						.getMessage()
						.contains("Subscription is not available to start the container for subject:"));
		final IOException nestedException = (IOException) exception.getCause();
		assertTrue(
				nestedException
						.getMessage()
						.contains("Timeout or no response waiting for NATS JetStream server"));
		// Assert that adapter and container is not running after start
		assertFalse(adapter.isRunning());
		assertFalse(container.isRunning());
	}
}
