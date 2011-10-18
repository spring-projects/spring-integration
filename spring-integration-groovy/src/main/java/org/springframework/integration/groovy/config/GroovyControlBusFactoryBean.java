/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.Message;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.groovy.GroovyCommandMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.CustomizableThreadCreator;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a Groovy Script.
 * 
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class GroovyControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	private volatile Long sendTimeout;

	private volatile GroovyObjectCustomizer customizer;


	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizer = customizer;
	}

	@Override
	protected MessageHandler createHandler() {
		ManagedBeansScriptVariableGenerator scriptVariableGenerator = new ManagedBeansScriptVariableGenerator(this.getBeanFactory());
		GroovyCommandMessageProcessor processor = new GroovyCommandMessageProcessor(scriptVariableGenerator);
		if (this.customizer != null) {
			processor.setCustomizer(this.customizer);
		}
		return this.configureHandler(new ServiceActivatingHandler(processor));
	}

	private ServiceActivatingHandler configureHandler(ServiceActivatingHandler handler) {
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}


	private static class ManagedBeansScriptVariableGenerator implements ScriptVariableGenerator {

		private final ConfigurableListableBeanFactory beanFactory;

		public ManagedBeansScriptVariableGenerator(BeanFactory beanFactory) {
			this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory)
					? (ConfigurableListableBeanFactory) beanFactory : null;
		}

		public Map<String, Object> generateScriptVariables(Message<?> message) {
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("headers", message.getHeaders());
			if (this.beanFactory != null) {
				for (String name : this.beanFactory.getBeanDefinitionNames()) {
					if (!this.beanFactory.getBeanDefinition(name).isAbstract()) {
						Object bean = this.beanFactory.getBean(name);
						if (bean instanceof Lifecycle ||
								bean instanceof CustomizableThreadCreator || 
								(AnnotationUtils.findAnnotation(bean.getClass(), ManagedResource.class) != null)) {
							variables.put(name, bean);
						}
					}
				}
			}
			return variables;
		}
	}

}
