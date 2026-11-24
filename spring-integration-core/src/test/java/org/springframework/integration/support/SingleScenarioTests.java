/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support;

import org.springframework.integration.test.support.PayloadValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.integration.test.support.SingleRequestResponseScenarioTest;
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
public class SingleScenarioTests extends SingleRequestResponseScenarioTest {

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
