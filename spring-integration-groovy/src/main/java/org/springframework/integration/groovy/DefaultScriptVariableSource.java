/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.groovy;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.util.CollectionUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.0.2
 */
class DefaultScriptVariableSource implements BeanFactoryAware, ScriptVariablesGenerator {
	
	protected volatile ListableBeanFactory beanFactory;
	
	private volatile Map<String, Object> variableMap;
	
	public DefaultScriptVariableSource(){
		this(null);
	}
	
	public DefaultScriptVariableSource(Map<String, Object> variableMap){
		this.variableMap = variableMap;
	}
	
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (beanFactory instanceof ListableBeanFactory) ? (ListableBeanFactory) beanFactory : null;
	}
	
	public Map<String, Object> generateScriptVariables(Message<?> message){
		Map<String, Object> scriptVariables = new HashMap<String, Object>();
		// Ad Message attributes
		if (message != null) {
			scriptVariables.put("payload", message.getPayload());
			scriptVariables.put("headers", message.getHeaders());
		}
		// Add contents of 'variableMap'
		if (!CollectionUtils.isEmpty(variableMap)){
			for (String variableName : variableMap.keySet()) {
				Object variableValue = variableMap.get(variableName);
				scriptVariables.put(variableName, variableValue);
			}		
		}
		return scriptVariables;
	}
}
