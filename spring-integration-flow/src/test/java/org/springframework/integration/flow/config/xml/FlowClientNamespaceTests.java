/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.flow.Flow;
import org.springframework.integration.test.support.AbstractRequestResponseScenarioTest;
import org.springframework.integration.test.support.MessageValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author David Turanski
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FlowClientNamespaceTests extends AbstractRequestResponseScenarioTest {

	@Autowired
	@Qualifier("flowWithProps")
	Flow flowWithProps;

	@Test
	public void testFlowWithInnerProps() {
		assertEquals("val1", flowWithProps.getProperties().getProperty("key1"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.integration.test.support.
	 * AbstractRequestResponseScenarioTest#defineRequestResponseScenarios()
	 */
	@Override
	protected List<RequestResponseScenario> defineRequestResponseScenarios() {
		List<RequestResponseScenario> scenarios = new ArrayList<RequestResponseScenario>();
		RequestResponseScenario scenario1 = new RequestResponseScenario("inputC1", "outputC1")
				.setName("gateway-response-scenario").setPayload("hello").setResponseValidator(new MessageValidator() {

					@Override
					protected void validateMessage(Message<?> message) {
						assertNotNull(message);

					}
				});

		scenarios.add(scenario1);

		RequestResponseScenario scenario2 = new RequestResponseScenario("inputC2", "outputC2")
				.setName("gateway-discard-scenario").setPayload("world").setResponseValidator(new MessageValidator() {

					@Override
					protected void validateMessage(Message<?> reply) {
						assertEquals("gateway-discard", reply.getHeaders().get("flow.output.port"));
						assertEquals("yeah!", reply.getHeaders().get("gateway"));

					}
				});

		scenarios.add(scenario2);

		return scenarios;
	}

}
