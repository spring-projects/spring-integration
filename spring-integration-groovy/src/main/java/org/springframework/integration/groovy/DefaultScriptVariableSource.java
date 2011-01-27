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
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.Message;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.CustomizableThreadCreator;

/**
 * @author Oleg Zhurakousky
 * @since 2.0.2
 */
public class DefaultScriptVariableSource implements BeanFactoryAware, ScriptVariableSource {
	
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
	
	public Map<String, Object> resolveScriptVariables(Message<?> message){
		Map<String, Object> scriptVariables = new HashMap<String, Object>();
		// Ad Message attributes
		if (message != null) {
			scriptVariables.put("payload", message.getPayload());
			scriptVariables.put("headers", message.getHeaders());
		}
		// Add contents of 'variableMap'
		if (!CollectionUtils.isEmpty(variableMap)){
			scriptVariables.putAll(variableMap);
		}
		// Add contents of 'beanFactory'
		if (this.beanFactory != null){
			for (String name : this.beanFactory.getBeanDefinitionNames()) {
				Object bean = this.beanFactory.getBean(name);
				if (bean instanceof Lifecycle || bean instanceof CustomizableThreadCreator
						|| (AnnotationUtils.findAnnotation(bean.getClass(), ManagedResource.class) != null)) {
					scriptVariables.put(name, bean);
				}
			}
		}
		
		this.doResolveScriptVariables(scriptVariables);
		return scriptVariables;
	}
	/**
	 * Will allow further customization to the map of script variables
	 * that will be accessible to script executing engine
	 * 
	 * @param variables
	 */
	protected void doResolveScriptVariables(Map<String, Object> variables){
		
	}
}
