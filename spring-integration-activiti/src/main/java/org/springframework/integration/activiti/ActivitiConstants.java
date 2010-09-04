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

/**
 * @author Josh Long
 */
public class ActivitiConstants {

	/**
	 * In order for the gateway to correctly signal execution to Activiti, it needs to the executionId so that it can look up the {@link  org.activiti.engine.runtime.Execution} instance.
	 * the <code>executionId</code> is expected to be under this header.
	 */
	public static final String WELL_KNOWN_EXECUTION_ID_HEADER_KEY = "activiti_spring_integration_executionId";

	/**
	 * Assuming #updateProcessVariablesFromReplyMessageHeaders is true, then any {@link org.springframework.integration.MessageHeaders} header key that starts with String will be propagated as an Activiti process variable.
	 */
	public static final String WELL_KNOWN_SPRING_INTEGRATION_HEADER_PREFIX = "activiti_spring_integration_";

	/**
	 * This is the key under which we will look up the custom <code>processDefinitionName</code> up. This value will be used to spawn
	 */
	public static final String WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY = "activiti_spring_integration_processDefinitionName";
}
