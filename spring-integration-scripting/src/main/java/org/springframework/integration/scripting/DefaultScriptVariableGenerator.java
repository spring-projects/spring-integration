/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
