/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.springframework.integration.http;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.core.Message;

/**
 * @author Dave Syer
 * @since 2.0
 *
 */
public class DefaultParameterMapper implements ParameterMapper {
	
	private static Log logger = LogFactory.getLog(DefaultParameterMapper.class);
	
	private SpelExpressionParser parser = new SpelExpressionParser();
	
	private Map<String, Expression> dynamicParameterExpressions = new HashMap<String, Expression>();
	
	/**
	 * A map of parameter name to SpEL expressions on the message.
	 * 
	 * @param dynamicParameterExpressions the dynamic parameter expressions to set
	 */
	public void setDynamicParameterExpressions(Map<String, String> dynamicParameterExpressions) {
		this.dynamicParameterExpressions.clear();
		for (String key : dynamicParameterExpressions.keySet()) {
			this.dynamicParameterExpressions.put(key, parser.parseExpression(dynamicParameterExpressions.get(key)));
		}
	}
	
	public Map<String, ?> fromMessage(Message<?> requestMessage) {

		Map<String, Object> params = new HashMap<String, Object>();
		for (String key : dynamicParameterExpressions.keySet()) {
			Object value = dynamicParameterExpressions.get(key).getValue(requestMessage);
			params.put(key, value);
		}

		if (requestMessage.getPayload() instanceof Map<?,?>) {
			Map<?,?> payloadMap = (Map<?,?>) requestMessage.getPayload();
			for (Object key : payloadMap.keySet()) {
				if (key instanceof String) {
					params.put((String) key, payloadMap.get(key).toString());
				}
				else if (logger.isDebugEnabled()) {
					logger.debug("ignoring Map value for non-String key: " + key);
				}
			}
		}

		return params;

	}

}
