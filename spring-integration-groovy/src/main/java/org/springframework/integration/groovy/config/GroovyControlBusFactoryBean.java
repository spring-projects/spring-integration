/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.Message;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.groovy.BeanFactoryGroovyVariableResolver;
import org.springframework.integration.groovy.FilteredBeanFactoryDecorator;
import org.springframework.integration.groovy.GroovyCommandMessageProcessor;
import org.springframework.integration.groovy.VariableResolver;
import org.springframework.integration.groovy.BeanFilter;
import org.springframework.integration.groovy.BeanFilterAdapter;
import org.springframework.integration.groovy.GroovyVariableResolverBinding;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.Assert;
import org.springframework.util.CustomizableThreadCreator;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a Groovy Script.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class GroovyControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	private volatile Long sendTimeout;

	private volatile GroovyObjectCustomizer customizer;

	private VariableResolver variableResolver;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizer = customizer;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.state(beanFactory != null, "BeanFactory is required");
		super.setBeanFactory(beanFactory);
		BeanFactory filteredBeanFactory = new FilteredBeanFactoryDecorator(beanFactory, new ControlBusBeanFilter());
		this.variableResolver = new BeanFactoryGroovyVariableResolver(filteredBeanFactory);
	}

	@Override
	protected MessageHandler createHandler() {
		ManagedBeansScriptVariableGenerator scriptVariableGenerator = new ManagedBeansScriptVariableGenerator(this.variableResolver);
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

	/**
	 * {@link ScriptVariableGenerator} implementation for 'Groovy Control Bus'.
	 * Adds {@link groovy.lang.Binding} 'binding' to the 'variables' map as strategy to resolve managed
	 * Spring beans by Groovy script variable at runtime.
	 */
	private static class ManagedBeansScriptVariableGenerator implements ScriptVariableGenerator {

		private final VariableResolver variableResolver;

		public ManagedBeansScriptVariableGenerator(VariableResolver variableResolver) {
			this.variableResolver = variableResolver;
		}

		public Map<String, Object> generateScriptVariables(Message<?> message) {
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("binding", new GroovyVariableResolverBinding(this.variableResolver));
			variables.put("headers", message.getHeaders());
			return variables;
		}
	}

	/**
	 * {@link BeanFilter} implementation for 'Groovy Control Bus' component. Allows to determine applicable
	 * managed Spring bean by Groovy 'variable' at runtime.
	 * Works in pair with {@link org.springframework.integration.groovy.FilteredBeanFactoryDecorator}.
	 */
	private static class ControlBusBeanFilter extends BeanFilterAdapter {

		public Object getBeanIfMatch(ConfigurableListableBeanFactory beanFactory, String name) {
			BeanDefinition def = beanFactory.getBeanDefinition(name);
			if (!def.isAbstract() && !def.isPrototype()) {
				Object bean = beanFactory.getBean(name);
				if (bean instanceof Lifecycle ||
						bean instanceof CustomizableThreadCreator ||
						(AnnotationUtils.findAnnotation(bean.getClass(), ManagedResource.class) != null)) {
					return bean;
				}
			}
			throw new BeanCreationNotAllowedException(name, "Only beans with @ManagedResource or beans which implement " +
					"org.springframework.context.Lifecycle or org.springframework.util.CustomizableThreadCreator " +
					"are allowed to use as ControlBus components.");
		}
	}
}
