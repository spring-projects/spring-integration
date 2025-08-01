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

package org.springframework.integration.test.support;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Convenience class for testing Spring Integration request-response message scenarios.
 * Users create subclasses to execute on or more {@link RequestResponseScenario} tests.
 * each scenario defines:
 * <ul>
 * <li>An inputChannelName</li>
 * <li>An outputChannelName</li>
 * <li>A payload or message to send as a request message on the inputChannel</li>
 * <li>A handler to validate the response received on the outputChannel</li>
 * </ul>
 *
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 * @author Jiandong Ma
 *
 * @since 7.0
 */
@SpringJUnitConfig
@DirtiesContext
public abstract class AbstractRequestResponseScenarioTest {

	@SuppressWarnings("NullAway.Init")
	private List<RequestResponseScenario> scenarios;

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeEach
	public void setUp() {
		this.scenarios = defineRequestResponseScenarios();
	}

	/**
	 * Execute each scenario. Instantiate the message channels, send the request message
	 * on the input channel and invoke the validator on the response received on the
	 * output channel. This can handle subscribable or pollable output channels.
	 */
	@Test
	public void testRequestResponseScenarios() {
		int i = 1;
		for (RequestResponseScenario scenario : scenarios) {
			String name = scenario.getName() == null ? "scenario-" + (i++) : scenario.getName();
			scenario.init();
			MessageChannel inputChannel = applicationContext.getBean(scenario.getInputChannelName(),
					MessageChannel.class);
			MessageChannel outputChannel = applicationContext.getBean(scenario.getOutputChannelName(),
					MessageChannel.class);
			AbstractResponseValidator<?> responseValidator = scenario.getResponseValidator();
			if (outputChannel instanceof SubscribableChannel subscribableChannel) {
				subscribableChannel.subscribe(responseValidator);
			}

			assertThat(inputChannel.send(scenario.getMessage()))
					.as(name + ": message not sent on " + scenario.getInputChannelName()).isTrue();

			if (outputChannel instanceof PollableChannel) {
				Message<?> response = ((PollableChannel) outputChannel).receive(10000); // NOSONAR magic number
				assertThat(response).as(name + ": receive timeout on " + scenario.getOutputChannelName()).isNotNull();
				responseValidator.handleMessage(Objects.requireNonNull(response));
			}

			assertThat(responseValidator.getLastMessage())
					.as("message was not handled on " + outputChannel + " for scenario '" + name + "'.").isNotNull();

			if (outputChannel instanceof SubscribableChannel) {
				((SubscribableChannel) outputChannel).unsubscribe(responseValidator);
			}
		}
	}

	/**
	 * Implement this method to define RequestResponse scenarios
	 * @return - A List of {@link RequestResponseScenario}
	 */
	protected abstract List<RequestResponseScenario> defineRequestResponseScenarios();

}
