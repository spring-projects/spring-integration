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
package org.springframework.integration.activiti.impls;

import org.springframework.integration.Message;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;


/**
 * This is a trivial component that demonstrates that the flow of control lives outside of the BPMN process (ie, that we have truly implemented a wait-state)
 * and that gateway does the right thing if you send back message headers whose key corresponds with process variables, or new message headers that you want as process variables.
 * <p/>
 * The only requirement for any component wishing to reply to an Activiti business process is that there be a header named using the static variable
 * {@link org.springframework.integration.activiti.ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY}.
 *
 * @author Josh Long
 */
public class DelayedchoServiceActivator {

	@ServiceActivator
	public Message<?> sayHello(Message<?> requestComingFromActiviti)
			throws Throwable {
		System.out.println("entering ServiceActivator:sayHello");
		Thread.sleep(5 * 1000);
		System.out.println("exiting ServiceActivator:sayHello");
		return MessageBuilder.withPayload(requestComingFromActiviti.getPayload()).
				copyHeadersIfAbsent(requestComingFromActiviti.getHeaders()).setHeader(
				ActivitiConstants.WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX + "test", "1 + 1").
				build();
	}
}
