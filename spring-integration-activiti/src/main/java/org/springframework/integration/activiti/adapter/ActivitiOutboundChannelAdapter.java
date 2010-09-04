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
package org.springframework.integration.activiti.adapter;

import org.activiti.engine.ProcessEngine;
import org.springframework.integration.Message;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.activiti.ProcessSupport;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

import java.util.Map;


/**
 * Supports spawning a {@link org.activiti.engine.runtime.ProcessInstance} as a result of a trigger {@link org.springframework.integration.Message}.
 * <p/>
 * The component also supports propagating headers as processVariables.
 * <p/>
 * This support is simialar to the classic EIP book's "Process Manager" pattern.
 * <p/>
 * Thanks to Mark Fisherfor the idea.
 *
 * @author Josh Long
 */
public class ActivitiOutboundChannelAdapter extends IntegrationObjectSupport implements MessageHandler {
	/**
	 * A reference to the {@link ProcessEngine} (see {@link org.activiti.engine.impl.cfg.spring.ProcessEngineFactoryBean}
	 */
	private ProcessEngine processEngine;

	/**
	 * Do you want all flows that come into this component to launch the same business process? Hard code the process name here.
	 * If this is null, the component will expect a well known header value and use that to spawn the process definition name.
	 */
	private String processDefinitionName;

	/**
	 * Provides convenience methods
	 */
	private ProcessSupport processSupport = new ProcessSupport();

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.processEngine, "'processEngine' is required!");
	}

	@SuppressWarnings("unused")
	public void setProcessEngine(ProcessEngine processEngine) {
		this.processEngine = processEngine;
	}

	@SuppressWarnings("unused")
	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public void handleMessage(Message<?> message) {
		Map<String, Object> procVars = processSupport.processVariablesFromMessageHeaders(message.getHeaders());

		String procName = (String) message.getHeaders().get(ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY);

		if ((procName == null) || procName.trim().equals("")) {
			procName = this.processDefinitionName;
		}

		Assert.isTrue(procName != null,
				"you must specify a processDefinitionName, either through " +
						"an inbound header mapped to the key " + ActivitiConstants.WELL_KNOWN_PROCESS_DEFINITION_NAME_HEADER_KEY +
						", or on the 'process-definition-name' property of this adapter"
		);

		processEngine.getRuntimeService().startProcessInstanceByKey(this.processDefinitionName, procVars);
	}
}
