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

package org.springframework.integration.support;

import org.springframework.integration.test.support.PayloadValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.integration.test.support.SingleRequestResponseScenarioTests;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration("MessageScenariosTests-context.xml")
public class SingleScenarioTests extends SingleRequestResponseScenarioTests {

	@Override
	protected RequestResponseScenario defineRequestResponseScenario() {
		RequestResponseScenario scenario = new RequestResponseScenario(
				"inputChannel", "outputChannel")
				.setPayload("hello")
				.setResponseValidator(new PayloadValidator<String>() {

					@Override
					protected void validateResponse(String response) {
						assertThat(response).isEqualTo("HELLO");
					}
				});
		return scenario;
	}

}
