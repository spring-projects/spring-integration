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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;


/**
 * Triggers a BPMN process that has process variables.
 *
 * @author Josh Long
 */
public class SimpleProcessTriggeringMessageSource implements MessageSource<String> {

	private final Log logger = LogFactory.getLog(SimpleProcessTriggeringMessageSource.class.getName());

	public Message<String> receive() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.debug("couldn't sleep 1000 ms", e);
		}
		return MessageBuilder.withPayload(
				"hello from " + System.currentTimeMillis()
		).
		setHeader(ActivitiConstants.WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX + "customerId", 232).
		setHeader( ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY, "helloWorldProcess" ).build();

	}
}
