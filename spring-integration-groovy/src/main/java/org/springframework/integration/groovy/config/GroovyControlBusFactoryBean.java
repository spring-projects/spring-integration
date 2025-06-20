/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.groovy.config;

import java.util.HashMap;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.MessageHandler;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.CustomizableThreadCreator;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a Groovy Script.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Stefan Reuter
 * @author Gary Russell
 *
 * @since 2.0
 *
 * @deprecated in favor of {@link org.springframework.integration.config.ControlBusFactoryBean}
 */
@Deprecated(since = "6.4", forRemoval = true)
public class GroovyControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler>
		implements BeanClassLoaderAware {

	private volatile Long sendTimeout;

	private volatile GroovyObjectCustomizer customizer;

	private volatile ClassLoader beanClassLoader;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizer = customizer;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	@SuppressWarnings("removal")
	protected MessageHandler createHandler() {
		Binding binding = new ManagedBeansBinding(this.getBeanFactory());
		org.springframework.integration.groovy.GroovyCommandMessageProcessor processor =
				new org.springframework.integration.groovy.GroovyCommandMessageProcessor(binding,
						message -> {
							Map<String, Object> variables = new HashMap<>();
							variables.put("headers", message.getHeaders());
							return variables;
						});
		if (this.customizer != null) {
			processor.setCustomizer(this.customizer);
		}
		if (this.beanClassLoader != null) {
			processor.setBeanClassLoader(this.beanClassLoader);
		}
		if (getBeanFactory() != null) {
			processor.setBeanFactory(getBeanFactory());
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
	 * Bridge {@link Binding} implementation which uses <code>beanFactory</code>
	 * to resolve Groovy variable as delegate if the last one isn't contained
	 * in the original Groovy script {@link Binding}.
	 * In additionally beans should be 'managed' with specific properties which
	 * are allowed in the Control Bus operations.
	 */
	private static final class ManagedBeansBinding extends Binding {

		private final ConfigurableListableBeanFactory beanFactory;

		private ManagedBeansBinding(BeanFactory beanFactory) {
			this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory)
					? (ConfigurableListableBeanFactory) beanFactory : null;
		}

		@Override
		public Object getVariable(String name) {
			try {
				return super.getVariable(name);
			}
			catch (MissingPropertyException e) {
//      Original {@link Binding} doesn't have 'variable' for the given 'name'.
//      Try to resolve it as 'managed bean' from the given <code>beanFactory</code>.
			}
			if (this.beanFactory == null) {
				throw new MissingPropertyException(name, this.getClass());
			}

			Object bean = null;
			try {
				bean = this.beanFactory.getBean(name);
			}
			catch (NoSuchBeanDefinitionException e) {
				throw new MissingPropertyException(name, this.getClass(), e);
			}

			if (bean instanceof Lifecycle ||
					bean instanceof CustomizableThreadCreator ||
					(AnnotationUtils.findAnnotation(bean.getClass(), ManagedResource.class) != null) ||
					(AnnotationUtils.findAnnotation(bean.getClass(), IntegrationManagedResource.class) != null)) {
				return bean;
			}
			throw new BeanCreationNotAllowedException(name,
					"Only beans with @ManagedResource or beans which implement " +
							"org.springframework.context.Lifecycle or org.springframework.util.CustomizableThreadCreator " +
							"are allowed to use as ControlBus components.");
		}

	}

}
