/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support;

import org.springframework.integration.test.support.PayloadValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.integration.test.support.SingleRequestResponseScenarioTests;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration("MessageScenariosTests-context.xml")
@DirtiesContext
public class SingleScenarioTests extends SingleRequestResponseScenarioTests {

	@Override
	protected RequestResponseScenario defineRequestResponseScenario() {
		return new RequestResponseScenario(
				"inputChannel", "outputChannel")
				.setPayload("hello")
				.setResponseValidator(new PayloadValidator<String>() {

					@Override
					protected void validateResponse(String response) {
						assertThat(response).isEqualTo("HELLO");
					}
				});
	}

}
