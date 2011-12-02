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

import org.junit.runner.RunWith;
import org.springframework.integration.Message;
import org.springframework.integration.test.support.MessageValidator;
import org.springframework.integration.test.support.RequestResponseScenario;
import org.springframework.integration.test.support.SingleRequestResponseScenarioTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 
/**
 * 
 * @author David Turanski
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FlowWithReferencesTests extends SingleRequestResponseScenarioTest { 

/* (non-Javadoc)
 * @see org.springframework.integration.test.support.SingleRequestResponseScenarioTest#defineRequestResponseScenario()
 */
@Override
protected RequestResponseScenario defineRequestResponseScenario() {
	RequestResponseScenario scenario = 
			new RequestResponseScenario("inputC","outputC")
		.setPayload("hello")
		.setResponseValidator(new MessageValidator() {

			@Override
			protected void validateMessage(Message<?> reply) {
				assertEquals("it works!",reply.getHeaders().get("refbean.value"));
			 	assertEquals("val1",reply.getHeaders().get("property.value.1"));
			 	assertEquals("undefined",reply.getHeaders().get("property.value.2"));
			}}
		);
	return scenario;
}
  
  
}
