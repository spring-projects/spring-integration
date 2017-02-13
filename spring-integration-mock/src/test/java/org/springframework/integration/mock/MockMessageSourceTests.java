/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.integration.mock.MockIntegration.mockMessageSource;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@MockIntegrationTest
@DirtiesContext
public class MockMessageSourceTests {

	@Autowired
	private MockIntegration.Context mockIntegrationContext;

	@Autowired
	private PollableChannel results;

	@After
	public void tearDown() {
		this.mockIntegrationContext.resetMocks();
	}

	@Test
	public void testMockMessageSource() {
		this.mockIntegrationContext.instead("mySourceEndpoint",
				mockMessageSource("foo", "bar", "baz"));

		Message<?> receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("BAR", receive.getPayload());

		receive = this.results.receive(10_000);
		assertNotNull(receive);
		assertEquals("BAZ", receive.getPayload());

		assertNull(this.results.receive(10));
	}

	@Configuration
	@EnableIntegration
	public static class RealConfig {

		@Bean
		public IntegrationFlow myFlow() {
			return IntegrationFlows
					.from(() -> new GenericMessage<>("myData"),
							e -> e.id("mySourceEndpoint")
									.poller(p -> p.fixedDelay(100))
									.autoStartup(false))
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("results"))
					.get();
		}

	}

}
