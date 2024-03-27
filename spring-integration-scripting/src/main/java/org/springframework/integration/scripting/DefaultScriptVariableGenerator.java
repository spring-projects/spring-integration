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

package org.springframework.integration.scripting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * A default {@link ScriptVariableGenerator} implementation; used by script processors.
 * The result of {@link #generateScriptVariables(Message)} is a {@link Map} of any provided {@code variables}
 * plus {@code payload} and {@code headers} from the {@code Message} argument.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0.2
 */
public class DefaultScriptVariableGenerator implements ScriptVariableGenerator {

	private final Map<String, Object> variableMap;

	public DefaultScriptVariableGenerator() {
		this.variableMap = Collections.<String, Object>emptyMap();
	}

	public DefaultScriptVariableGenerator(Map<String, Object> variableMap) {
		this.variableMap = variableMap;
	}

	public Map<String, Object> generateScriptVariables(Message<?> message) {
		Map<String, Object> scriptVariables = new HashMap<String, Object>();
		// Add Message content
		if (message != null) {
			scriptVariables.put("payload", message.getPayload());
			scriptVariables.put("headers", message.getHeaders());
		}
		// Add contents of 'variableMap'
		if (!CollectionUtils.isEmpty(this.variableMap)) {
			for (Map.Entry<String, Object> entry : this.variableMap.entrySet()) {
				scriptVariables.put(entry.getKey(), entry.getValue());
			}
		}
		return scriptVariables;
	}

}
