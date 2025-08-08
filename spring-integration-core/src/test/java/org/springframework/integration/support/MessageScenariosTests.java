/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.integration.test.support.AbstractRequestResponseScenarioTests;
import org.springframework.integration.test.support.MessageValidator;
import org.springframework.integration.test.support.PayloadValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@DirtiesContext
public class MessageScenariosTests extends AbstractRequestResponseScenarioTests {

	@Override
	protected List<RequestResponseScenario> defineRequestResponseScenarios() {
		List<RequestResponseScenario> scenarios = new ArrayList<>();
		RequestResponseScenario scenario1 = new RequestResponseScenario(
				"inputChannel", "outputChannel")
				.setPayload("hello")
				.setResponseValidator(new PayloadValidator<String>() {

					@Override
					protected void validateResponse(String response) {
						assertThat(response).isEqualTo("HELLO");
					}
				});

		scenarios.add(scenario1);

		Message<?> resultMessage = MessageBuilder.withPayload("HELLO").setHeader("foo", "bar").build();

		RequestResponseScenario scenario2 = new RequestResponseScenario(
				"inputChannel", "outputChannel")
				.setMessage(MessageBuilder.withPayload("hello").setHeader("foo", "bar").build())
				.setResponseValidator(new MessageValidator() {

					@Override
					protected void validateMessage(Message<?> message) {
						assertThat(message).matches(new MessagePredicate(resultMessage));
					}
				});

		scenarios.add(scenario2);

		RequestResponseScenario scenario3 = new RequestResponseScenario(
				"inputChannel2", "outputChannel2")
				.setMessage(MessageBuilder.withPayload("hello").setHeader("foo", "bar").build())
				.setResponseValidator(new MessageValidator() {

					@Override
					protected void validateMessage(Message<?> message) {
						assertThat(message).matches(new MessagePredicate(resultMessage));
					}
				});

		scenarios.add(scenario3);

		return scenarios;
	}

}
