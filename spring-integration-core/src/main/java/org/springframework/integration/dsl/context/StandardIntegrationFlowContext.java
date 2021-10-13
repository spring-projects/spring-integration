/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Standard implementation of {@link IntegrationFlowContext}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Alexander Shaklein
 * @author Artem Vozhdayenko
 *
 * @since 5.1
 *
 */
public final class StandardIntegrationFlowContext implements IntegrationFlowContext, BeanFactoryAware {

	private final Map<String, IntegrationFlowRegistration> registry = new ConcurrentHashMap<>();

	private final Map<String, Boolean> useFlowIdAsPrefix = new ConcurrentHashMap<>();

	private final Lock registerFlowsLock = new ReentrantLock();

	private ConfigurableListableBeanFactory beanFactory;

	private BeanDefinitionRegistry beanDefinitionRegistry;

	StandardIntegrationFlowContext() {
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"To use Spring Integration Java DSL the 'beanFactory' has to be an instance of " +
						"'ConfigurableListableBeanFactory'. " +
						"Consider using 'GenericApplicationContext' implementation.");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.beanDefinitionRegistry = (BeanDefinitionRegistry) this.beanFactory;
	}

	/**
	 * Associate provided {@link IntegrationFlow} with an {@link StandardIntegrationFlowRegistrationBuilder}
	 * for additional options and farther registration in the application context.
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 * @return the IntegrationFlowRegistrationBuilder associated with the provided {@link IntegrationFlow}
	 */
	@Override
	public StandardIntegrationFlowRegistrationBuilder registration(IntegrationFlow integrationFlow) {
		return new StandardIntegrationFlowRegistrationBuilder(integrationFlow);
	}

	@Override
	public boolean isUseIdAsPrefix(String flowId) {
		return Boolean.TRUE.equals(this.useFlowIdAsPrefix.get(flowId));
	}

	private IntegrationFlowRegistration register(StandardIntegrationFlowRegistrationBuilder builder) {
		IntegrationFlow integrationFlow = builder.integrationFlow;
		String flowId = builder.id;
		Lock registerBeanLock = null;
		try {
			if (flowId == null) {
				registerBeanLock = this.registerFlowsLock;
				registerBeanLock.lock();
				flowId = generateBeanName(integrationFlow, null);
				builder.id(flowId);
			}
			else if (this.registry.containsKey(flowId)) {
				throw new IllegalArgumentException("An IntegrationFlow '" + this.registry.get(flowId) +
						"' with flowId '" + flowId + "' is already registered.\n" +
						"An existing IntegrationFlowRegistration must be destroyed before overriding.");
			}

			integrationFlow = registerFlowBean(integrationFlow, flowId, builder.source);
		}
		finally {
			if (registerBeanLock != null) {
				registerBeanLock.unlock();
			}
		}

		builder.integrationFlow = integrationFlow;

		final String theFlowId = flowId;
		builder.additionalBeans.forEach((key, value) -> registerBean(key, value, theFlowId));

		IntegrationFlowRegistration registration =
				new StandardIntegrationFlowRegistration(integrationFlow, this, flowId);
		if (builder.autoStartup) {
			registration.start();
			builder.additionalBeans.keySet()
					.stream()
					.filter(SmartLifecycle.class::isInstance)
					.filter((lifecycle) -> ((SmartLifecycle) lifecycle).isAutoStartup())
					.forEach((lifecycle) -> ((SmartLifecycle) lifecycle).start());
		}
		this.registry.put(flowId, registration);

		return registration;
	}

	private IntegrationFlow registerFlowBean(IntegrationFlow flow, String flowId, @Nullable Object source) {
		AbstractBeanDefinition beanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition(IntegrationFlow.class, () -> flow)
						.getRawBeanDefinition();
		beanDefinition.setSource(source);
		this.beanDefinitionRegistry.registerBeanDefinition(flowId, beanDefinition);
		return this.beanFactory.getBean(flowId, IntegrationFlow.class);
	}

	@SuppressWarnings("unchecked")
	private void registerBean(Object bean, @Nullable String beanNameArg, String parentName) {
		String beanName = beanNameArg;
		if (beanName == null) {
			beanName = generateBeanName(bean, parentName);
		}

		AbstractBeanDefinition beanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) bean.getClass(), () -> bean)
						.getRawBeanDefinition();

		if (parentName != null) {
			AbstractBeanDefinition parentBeanDefinition =
					(AbstractBeanDefinition) this.beanFactory.getBeanDefinition(parentName);
			Object source = parentBeanDefinition.getSource();
			if (source instanceof MethodMetadata) {
				source = "bean method " + ((MethodMetadata) source).getMethodName();
			}
			beanDefinition.setSource(source);
			this.beanFactory.registerDependentBean(parentName, beanName);
		}
		this.beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
		this.beanFactory.getBean(beanName);
	}

	/**
	 * Obtain an {@link IntegrationFlowRegistration} for the {@link IntegrationFlow}
	 * associated with the provided {@code flowId}.
	 * @param flowId the bean name to obtain
	 * @return the IntegrationFlowRegistration for provided {@code id} or {@code null}
	 */
	@Override
	public IntegrationFlowRegistration getRegistrationById(String flowId) {
		return this.registry.get(flowId);
	}

	/**
	 * Destroy an {@link IntegrationFlow} bean (as well as all its dependant beans)
	 * for provided {@code flowId} and clean up all the local cache for it.
	 * @param flowId the bean name to destroy from
	 */
	@Override
	public void remove(String flowId) {
		final IntegrationFlowRegistration flowRegistration = this.registry.remove(flowId);
		if (flowRegistration != null) {
			flowRegistration.stop();

			removeDependantBeans(flowId);

			this.beanDefinitionRegistry.removeBeanDefinition(flowId);
		}
		else {
			throw new IllegalStateException("An IntegrationFlow with the id "
					+ "[" + flowId + "] doesn't exist in the registry.");
		}
	}

	private void removeDependantBeans(String parentName) {
		String[] dependentBeans = this.beanFactory.getDependentBeans(parentName);
		for (String beanName : dependentBeans) {
			removeDependantBeans(beanName);
			this.beanDefinitionRegistry.removeBeanDefinition(beanName);
			// TODO until https://jira.spring.io/browse/SPR-16837
			String[] aliases = this.beanDefinitionRegistry.getAliases(beanName);
			for (String alias : aliases) {
				this.beanDefinitionRegistry.removeAlias(alias);
			}
		}
	}

	/**
	 * Obtain a {@link MessagingTemplate} with its default destination set to the input channel
	 * of the {@link IntegrationFlow} for provided {@code flowId}.
	 * <p> Any {@link IntegrationFlow} bean (not only manually registered) can be used for this method.
	 * <p> If {@link IntegrationFlow} doesn't start with the
	 * {@link org.springframework.messaging.MessageChannel}, the
	 * {@link IllegalStateException} is thrown.
	 * @param flowId the bean name to obtain the input channel from
	 * @return the {@link MessagingTemplate} instance
	 */
	@Override
	public MessagingTemplate messagingTemplateFor(String flowId) {
		return this.registry.get(flowId)
				.getMessagingTemplate();
	}

	/**
	 * Provide the state of the mapping of integration flow names to their
	 * {@link IntegrationFlowRegistration} instances.
	 * @return the registry of flow ids and their registration.
	 */
	@Override
	public Map<String, IntegrationFlowRegistration> getRegistry() {
		return Collections.unmodifiableMap(this.registry);
	}

	private String generateBeanName(Object instance, String parentName) {
		if (instance instanceof NamedComponent) {
			String beanName = ((NamedComponent) instance).getBeanName();
			if (beanName != null) {
				return beanName;
			}
		}

		String generatedBeanName = (parentName != null ? parentName : "") + instance.getClass().getName();
		String id = generatedBeanName;
		int counter = -1;
		while (counter == -1 || this.beanFactory.containsBean(id)) {
			counter++;
			id = generatedBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
		}
		return id;
	}

	/**
	 * A Builder pattern implementation for the options to register {@link IntegrationFlow}
	 * in the application context.
	 */
	public final class StandardIntegrationFlowRegistrationBuilder implements IntegrationFlowRegistrationBuilder {

		private final Map<Object, String> additionalBeans = new HashMap<>();

		private IntegrationFlow integrationFlow;

		private String id;

		private boolean autoStartup = true;

		private boolean idAsPrefix;

		@Nullable
		private Object source;

		StandardIntegrationFlowRegistrationBuilder(IntegrationFlow integrationFlow) {
			this.integrationFlow = integrationFlow;
		}

		/**
		 * Specify an {@code id} for the {@link IntegrationFlow} to register.
		 * Must be unique per context.
		 * The registration with this {@code id} must be destroyed before reusing for
		 * a new {@link IntegrationFlow} instance.
		 * @param id the id for the {@link IntegrationFlow} to register
		 * @return the current builder instance
		 */
		@Override
		public StandardIntegrationFlowRegistrationBuilder id(String id) {
			this.id = id;
			return this;
		}

		/**
		 * The {@code boolean} flag to indication if an {@link IntegrationFlow} must be
		 * started automatically after registration. Defaults to {@code true}.
		 * @param autoStartupToSet start or not the {@link IntegrationFlow} automatically
		 * after registration.
		 * @return the current builder instance
		 */
		@Override
		public StandardIntegrationFlowRegistrationBuilder autoStartup(boolean autoStartupToSet) {
			this.autoStartup = autoStartupToSet;
			return this;
		}

		/**
		 * Add an object which will be registered as an {@link IntegrationFlow} dependant bean in the
		 * application context. Usually it is some support component, which needs an application context.
		 * For example dynamically created connection factories or header mappers for AMQP, JMS, TCP etc.
		 * @param bean an additional arbitrary bean to register into the application context.
		 * @return the current builder instance
		 */
		@Override
		public StandardIntegrationFlowRegistrationBuilder addBean(Object bean) {
			return addBean(null, bean);
		}

		/**
		 * Add an object which will be registered as an {@link IntegrationFlow} dependant bean in the
		 * application context. Usually it is some support component, which needs an application context.
		 * For example dynamically created connection factories or header mappers for AMQP, JMS, TCP etc.
		 * @param name the name for the bean to register.
		 * @param bean an additional arbitrary bean to register into the application context.
		 * @return the current builder instance
		 */
		@Override
		public StandardIntegrationFlowRegistrationBuilder addBean(String name, Object bean) {
			this.additionalBeans.put(bean, name);
			return this;
		}

		@Override
		public IntegrationFlowRegistrationBuilder setSource(Object source) {
			this.source = source;
			return this;
		}

		@Override
		public IntegrationFlowRegistrationBuilder useFlowIdAsPrefix() {
			this.idAsPrefix = true;
			return this;
		}

		/**
		 * Register an {@link IntegrationFlow} and all the dependant and support components
		 * in the application context and return an associated {@link IntegrationFlowRegistration}
		 * control object.
		 * @return the {@link IntegrationFlowRegistration} instance.
		 */
		@Override
		public IntegrationFlowRegistration register() {
			Assert.state(!this.idAsPrefix || StringUtils.hasText(this.id),
					"An 'id' must be present to use 'useFlowIdAsPrefix'");
			if (this.idAsPrefix) {
				StandardIntegrationFlowContext.this.useFlowIdAsPrefix.put(this.id, this.idAsPrefix);
			}
			IntegrationFlowRegistration registration = StandardIntegrationFlowContext.this.register(this);
			registration.setBeanFactory(StandardIntegrationFlowContext.this.beanFactory);
			return registration;
		}

	}

}
