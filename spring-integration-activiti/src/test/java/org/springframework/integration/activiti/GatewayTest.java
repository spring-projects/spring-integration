/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.springframework.integration.activiti;

import org.activiti.engine.ProcessEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;


/**
 * This component demonstrates creating a {@link org.springframework.integration.activiti.gateway.AsyncActivityBehaviorMessagingGateway} (factoried from Spring)
 * and exposed for use in a BPMN 2 process.
 *
 * @author Josh Long
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayTest extends AbstractJUnit4SpringContextTests {

	@Autowired private ProcessEngine processEngine ;

	@Test
	public void testGateway() throws Throwable {
		processEngine.getRepositoryService().createDeployment().addClasspathResource("processes/si_gateway_example.bpmn20.xml").deploy();
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("customerId", 232);
		processEngine.getRuntimeService().startProcessInstanceByKey("sigatewayProcess", vars);
		Thread.sleep(10000);

	}
}
