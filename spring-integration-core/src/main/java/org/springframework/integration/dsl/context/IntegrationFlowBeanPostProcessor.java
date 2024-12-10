/*
 * Copyright 2016-2024 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.context.ComponentSourceAware;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.ConsumerEndpointSpec;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.gateway.AnnotationGatewayProxyFactoryBean;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * A {@link BeanPostProcessor} to parse {@link IntegrationFlow} beans and register their
 * components as beans in the provided
 * {@link org.springframework.beans.factory.BeanFactory}, if necessary.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class IntegrationFlowBeanPostProcessor
		implements BeanPostProcessor, ApplicationContextAware, SmartInitializingSingleton, AopInfrastructureBean {

	private ConfigurableApplicationContext applicationContext;

	private StringValueResolver embeddedValueResolver;

	private DefaultListableBeanFactory beanFactory;

	private volatile IntegrationFlowContext flowContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
				"To use Spring Integration Java DSL the 'applicationContext' has to be an instance of " +
						"'ConfigurableApplicationContext'. Consider using 'GenericApplicationContext' implementation."
		);

		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
		this.beanFactory = (DefaultListableBeanFactory) this.applicationContext.getBeanFactory();
		this.embeddedValueResolver = new EmbeddedValueResolver(this.beanFactory);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof StandardIntegrationFlow standardIntegrationFlow) {
			return processStandardIntegrationFlow(standardIntegrationFlow, beanName);
		}
		else if (bean instanceof IntegrationFlow integrationFlow) {
			return processIntegrationFlowImpl(integrationFlow, beanName);
		}
		if (bean instanceof IntegrationComponentSpec<?, ?> integrationComponentSpec) {
			processIntegrationComponentSpec(beanName, integrationComponentSpec);
		}
		return bean;
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (String beanName : this.beanFactory.getBeanNamesForType(IntegrationFlow.class)) {
			if (this.beanFactory.containsBeanDefinition(beanName)) {
				String scope = this.beanFactory.getBeanDefinition(beanName).getScope();
				if (StringUtils.hasText(scope) && !BeanDefinition.SCOPE_SINGLETON.equals(scope)) {
					throw new BeanCreationNotAllowedException(beanName, "IntegrationFlows can not be scoped beans. " +
							"Any dependant beans are registered as singletons, meanwhile IntegrationFlow is just a " +
							"logical container for them. \n" +
							"Consider using [IntegrationFlowContext] for manual registration of IntegrationFlows.");
				}
			}
		}
	}

	private Object processStandardIntegrationFlow(StandardIntegrationFlow flow, String flowBeanName) { // NOSONAR complexity
		boolean registerBeanDefinitions = this.beanFactory.containsBeanDefinition(flowBeanName);
		if (registerBeanDefinitions) {
			BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(flowBeanName);
			flow.setComponentSource(beanDefinition.getSource());
			flow.setComponentDescription(beanDefinition.getDescription());
		}
		Object beanSource = flow.getComponentSource();
		String beanDescription = flow.getComponentDescription();

		String flowNamePrefix = flowBeanName + ".";
		if (this.flowContext == null) {
			this.flowContext = this.beanFactory.getBean(IntegrationFlowContext.class);
		}
		boolean useFlowIdAsPrefix = this.flowContext.isUseIdAsPrefix(flowBeanName);
		int subFlowNameIndex = 0;
		int channelNameIndex = 0;

		Map<Object, String> integrationComponents = flow.getIntegrationComponents();
		Map<Object, String> targetIntegrationComponents = new LinkedHashMap<>(integrationComponents.size());

		for (Map.Entry<Object, String> entry : integrationComponents.entrySet()) {
			Object component = entry.getKey();
			if (component instanceof ConsumerEndpointSpec<?, ?> endpointSpec) {
				MessageHandler messageHandler = endpointSpec.getObject().getT2();
				ConsumerEndpointFactoryBean endpoint = endpointSpec.getObject().getT1();
				String id = endpointSpec.getId();

				if (id == null) {
					id = generateBeanName(endpoint, flowNamePrefix, entry.getValue(), useFlowIdAsPrefix);
				}
				else if (useFlowIdAsPrefix) {
					id = flowNamePrefix + id;
				}

				if (noBeanPresentForComponent(messageHandler, flowBeanName)) {
					String handlerBeanName = generateBeanName(messageHandler, flowNamePrefix);

					registerComponent(registerBeanDefinitions, messageHandler, handlerBeanName,
							beanSource, beanDescription, flowBeanName);
					this.beanFactory.registerAlias(handlerBeanName, id + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX);
				}

				registerComponent(registerBeanDefinitions, endpoint, id, beanSource,
						beanDescription, flowBeanName);
				targetIntegrationComponents.put(endpoint, id);
			}
			else if (component instanceof MessageChannelReference messageChannelReference) {
				String channelBeanName = messageChannelReference.name();
				MessageChannel channelByName;
				if (!this.beanFactory.containsBean(channelBeanName)) {
					channelByName = new DirectChannel();
					registerComponent(registerBeanDefinitions, channelByName, channelBeanName,
							beanSource, beanDescription, flowBeanName);
				}
				else {
					channelByName = this.beanFactory.getBean(channelBeanName, MessageChannel.class);
				}
				targetIntegrationComponents.put(channelByName, channelBeanName);
			}
			else if (component instanceof SourcePollingChannelAdapterSpec spec) {
				Map<Object, String> componentsToRegister = spec.getComponentsToRegister();
				if (!CollectionUtils.isEmpty(componentsToRegister)) {
					componentsToRegister.entrySet()
							.stream()
							.filter(o -> noBeanPresentForComponent(o.getKey(), flowBeanName))
							.forEach(o ->
									registerComponent(registerBeanDefinitions, o.getKey(),
											generateBeanName(o.getKey(), flowNamePrefix, o.getValue(),
													useFlowIdAsPrefix), beanSource, beanDescription, spec.getId()));
				}
				SourcePollingChannelAdapterFactoryBean pollingChannelAdapterFactoryBean = spec.getObject().getT1();
				String id = spec.getId();
				if (id == null) {
					id = generateBeanName(pollingChannelAdapterFactoryBean, flowNamePrefix, entry.getValue(),
							useFlowIdAsPrefix);
				}
				else if (useFlowIdAsPrefix) {
					id = flowNamePrefix + id;
				}

				registerComponent(registerBeanDefinitions, pollingChannelAdapterFactoryBean, id,
						beanSource, beanDescription, flowBeanName);
				targetIntegrationComponents.put(pollingChannelAdapterFactoryBean, id);

				MessageSource<?> messageSource = spec.getObject().getT2();
				if (noBeanPresentForComponent(messageSource, flowBeanName)) {
					String messageSourceId = id + ".source";
					if (messageSource instanceof NamedComponent namedComponent
							&& namedComponent.getComponentName() != null) {

						messageSourceId = namedComponent.getComponentName();
					}
					registerComponent(registerBeanDefinitions, messageSource, messageSourceId,
							beanSource, beanDescription, flowBeanName);
				}
			}
			else {
				if (noBeanPresentForComponent(component, flowBeanName)) {
					if (component instanceof AbstractMessageChannel || component instanceof NullChannel) {
						String channelBeanName = ((NamedComponent) component).getComponentName();
						if (channelBeanName == null) {
							channelBeanName = entry.getValue();
							if (channelBeanName == null) {
								channelBeanName = flowNamePrefix + "channel" +
										BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + channelNameIndex++;
							}
						}
						registerComponent(registerBeanDefinitions, component, channelBeanName,
								beanSource, beanDescription, flowBeanName);
						targetIntegrationComponents.put(component, channelBeanName);
					}
					else if (component instanceof FixedSubscriberChannel fixedSubscriberChannel) {
						String channelBeanName = fixedSubscriberChannel.getComponentName();
						if ("Unnamed fixed subscriber channel".equals(channelBeanName)) {
							channelBeanName = flowNamePrefix + "channel" +
									BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + channelNameIndex++;
						}
						registerComponent(registerBeanDefinitions, component, channelBeanName,
								beanSource, beanDescription, flowBeanName);
						targetIntegrationComponents.put(component, channelBeanName);
					}
					else if (component instanceof StandardIntegrationFlow) {
						String subFlowBeanName =
								entry.getValue() != null
										? entry.getValue()
										: flowNamePrefix + "subFlow" +
										BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + subFlowNameIndex++;
						registerComponent(registerBeanDefinitions, component, subFlowBeanName,
								beanSource, beanDescription, flowBeanName);
						targetIntegrationComponents.put(component, subFlowBeanName);
					}
					else if (component instanceof AnnotationGatewayProxyFactoryBean<?> gateway) {
						String gatewayId = entry.getValue();

						if (gatewayId == null) {
							gatewayId = gateway.getComponentName();
						}
						if (gatewayId == null) {
							gatewayId = flowNamePrefix + "gateway";
						}

						if (registerBeanDefinitions) {
							registerBeanDefinition(gateway, gatewayId, beanSource,
									beanDescription, flowBeanName,
									beanDefinition -> {
										RootBeanDefinition definition = (RootBeanDefinition) beanDefinition;
										Class<?> serviceInterface = gateway.getObjectType();
										definition.setSource(new DescriptiveResource("" + serviceInterface));
										definition.setTargetType(
												ResolvableType.forClassWithGenerics(AnnotationGatewayProxyFactoryBean.class,
														serviceInterface));
									});
						}
						else {
							registerSingleton(gateway, gatewayId, beanSource, beanDescription, flowBeanName);
						}

						targetIntegrationComponents.put(component, gatewayId);
					}
					else {
						String generatedBeanName =
								generateBeanName(component, flowNamePrefix, entry.getValue(), useFlowIdAsPrefix);

						registerComponent(registerBeanDefinitions, component, generatedBeanName,
								beanSource, beanDescription, flowBeanName);
						targetIntegrationComponents.put(component, generatedBeanName);
					}
				}
				else {
					Object componentToUse = entry.getKey();
					String beanNameToUse = entry.getValue();
					if (StringUtils.hasText(beanNameToUse) &&
							ConfigurableBeanFactory.SCOPE_PROTOTYPE.equals(
									IntegrationContextUtils.getBeanDefinition(beanNameToUse, this.beanFactory)
											.getScope())) {

						this.beanFactory.initializeBean(componentToUse, beanNameToUse);
					}
					targetIntegrationComponents.put(component, beanNameToUse);
				}
			}
		}

		flow.setIntegrationComponents(targetIntegrationComponents);
		return flow;
	}

	/**
	 * Only invoked for {@link IntegrationFlow} instances that are not
	 * {@link StandardIntegrationFlow}s; typically lambdas. Creates a new
	 * {@link StandardIntegrationFlow} with an input channel named {@code beanName.input}
	 * and the flow defined by the flow parameter. If the flow is not an
	 * {@link IntegrationFlowAdapter} the original, user-provided {@link IntegrationFlow}
	 * is wrapped in a proxy and advised with a {@link IntegrationFlowLifecycleAdvice};
	 * see its javadocs for more information.
	 */
	private Object processIntegrationFlowImpl(IntegrationFlow flow, String beanName) {
		IntegrationFlowBuilder flowBuilder = IntegrationFlow.from(beanName + ".input");

		flow.configure(flowBuilder);

		StandardIntegrationFlow target = flowBuilder.get();
		processStandardIntegrationFlow(target, beanName);

		if (!(flow instanceof IntegrationFlowAdapter)) {
			NameMatchMethodPointcutAdvisor integrationFlowAdvice =
					new NameMatchMethodPointcutAdvisor(new IntegrationFlowLifecycleAdvice(target));
			integrationFlowAdvice.setMappedNames(
					"getInputChannel",
					"getIntegrationComponents",
					"start",
					"stop",
					"isRunning",
					"isAutoStartup",
					"getPhase");

			ProxyFactory proxyFactory = new ProxyFactory(flow);
			proxyFactory.addAdvisor(integrationFlowAdvice);
			if (!(flow instanceof SmartLifecycle)) {
				proxyFactory.addInterface(SmartLifecycle.class);
			}
			return proxyFactory.getProxy(this.beanFactory.getBeanClassLoader());
		}
		else {
			return flow;
		}
	}

	private void processIntegrationComponentSpec(String beanName, IntegrationComponentSpec<?, ?> bean) {
		Object target = bean.getObject();

		invokeBeanInitializationHooks(beanName, target);

		if (bean instanceof ComponentsRegistration componentsRegistration) {
			Map<Object, String> componentsToRegister = componentsRegistration.getComponentsToRegister();
			if (!CollectionUtils.isEmpty(componentsToRegister)) {
				boolean registerBeanDefinitions = this.beanFactory.containsBeanDefinition(beanName);
				BeanDefinition beanDefinition = null;
				if (registerBeanDefinitions) {
					beanDefinition = this.beanFactory.getBeanDefinition(beanName);
				}
				Object beanDefinitionSource = registerBeanDefinitions ? beanDefinition.getSource() : null;
				String beanDefinitionDescription = registerBeanDefinitions ? beanDefinition.getDescription() : null;
				componentsToRegister.entrySet()
						.stream()
						.filter(component -> noBeanPresentForComponent(component.getKey(), beanName))
						.forEach(component ->
								registerComponent(registerBeanDefinitions, component.getKey(),
										generateBeanName(component.getKey(), beanName, component.getValue(), false),
										beanDefinitionSource, beanDefinitionDescription, beanName));
			}
		}
	}

	private void invokeBeanInitializationHooks(final String beanName, final Object bean) { // NOSONAR complexity
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware beanNameAware) {
				beanNameAware.setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware beanClassLoaderAware
					&& this.beanFactory.getBeanClassLoader() != null) {

				beanClassLoaderAware.setBeanClassLoader(this.beanFactory.getBeanClassLoader()); // NOSONAR
			}
			if (bean instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(this.beanFactory);
			}
			if (bean instanceof EnvironmentAware environmentAware) {
				environmentAware.setEnvironment(this.applicationContext.getEnvironment());
			}
			if (bean instanceof EmbeddedValueResolverAware embeddedValueResolverAware) {
				embeddedValueResolverAware.setEmbeddedValueResolver(this.embeddedValueResolver);
			}
			if (bean instanceof ResourceLoaderAware resourceLoaderAware) {
				resourceLoaderAware.setResourceLoader(this.applicationContext);
			}
			if (bean instanceof ApplicationEventPublisherAware eventPublisherAware) {
				eventPublisherAware.setApplicationEventPublisher(this.applicationContext);
			}
			if (bean instanceof MessageSourceAware messageSourceAware) {
				messageSourceAware.setMessageSource(this.applicationContext);
			}
			if (bean instanceof ApplicationContextAware applicationContextAware) {
				applicationContextAware.setApplicationContext(this.applicationContext);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean noBeanPresentForComponent(Object instance, String parentBeanName) {
		if (instance instanceof NamedComponent namedComponent) {
			String beanName = namedComponent.getBeanName();
			if (beanName == null || !this.beanFactory.containsBean(beanName)) {
				return true;
			}
			else {
				BeanDefinition existingBeanDefinition = null;
				try {
					existingBeanDefinition = IntegrationContextUtils.getBeanDefinition(beanName, this.beanFactory);
				}
				catch (NoSuchBeanDefinitionException ex) {
					// Ignore and move on as no bean definition (possibly just singleton?)
				}

				if (existingBeanDefinition == null
						|| !ConfigurableBeanFactory.SCOPE_PROTOTYPE.equals(existingBeanDefinition.getScope())) {

					Object existingBean = this.beanFactory.getBean(beanName);
					if (!instance.equals(existingBean)) {
						AbstractBeanDefinition beanDefinition =
								BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) instance.getClass(),
												() -> instance)
										.getBeanDefinition();
						beanDefinition.setResourceDescription("the '" + parentBeanName + "' bean definition");
						if (existingBeanDefinition == null) {
							existingBeanDefinition =
									BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) existingBean.getClass(),
													() -> existingBean)
											.getBeanDefinition();
						}
						throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingBeanDefinition);
					}
				}
				return false;
			}
		}

		return !this.beanFactory.getBeansOfType(instance.getClass(), false, false)
				.containsValue(instance);
	}

	private void registerComponent(boolean registerBeanDefinition,
			Object component, String beanName, @Nullable Object source,
			@Nullable String description, @Nullable String parentName) {

		if (registerBeanDefinition) {
			registerBeanDefinition(component, beanName, source, description, parentName);
		}
		else {
			registerSingleton(component, beanName, source, description, parentName);
		}
	}

	@SuppressWarnings("unchecked")
	private void registerBeanDefinition(Object component, String beanName, @Nullable Object source,
			@Nullable String description, @Nullable String parentName,
			BeanDefinitionCustomizer... customizers) {

		AbstractBeanDefinition beanDefinition =
				BeanDefinitionBuilder.rootBeanDefinition((Class<Object>) component.getClass(), () -> component)
						.applyCustomizers(customizers)
						.getRawBeanDefinition();

		beanDefinition.setSource(source);
		beanDefinition.setDescription(description);

		if (parentName != null) {
			this.beanFactory.registerDependentBean(parentName, beanName);
		}

		((BeanDefinitionRegistry) this.beanFactory).registerBeanDefinition(beanName, beanDefinition);
		this.beanFactory.getBean(beanName);
	}

	private void registerSingleton(Object component, String beanName, @Nullable Object source,
			@Nullable String description, @Nullable String parentName) {

		if (component instanceof ComponentSourceAware componentSourceAware) {
			componentSourceAware.setComponentSource(source);
			componentSourceAware.setComponentDescription(description);
		}

		if (parentName != null) {
			this.beanFactory.registerDependentBean(parentName, beanName);
		}

		this.beanFactory.registerSingleton(beanName, component);
		this.beanFactory.initializeBean(component, beanName);
		this.beanFactory.getBean(beanName);
	}

	private String generateBeanName(Object instance, String prefix) {
		return generateBeanName(instance, prefix, null, false);
	}

	private String generateBeanName(Object instance, String prefix, @Nullable String fallbackId,
			boolean useFlowIdAsPrefix) {

		if (instance instanceof NamedComponent namedComponent && namedComponent.getBeanName() != null) {
			String beanName = namedComponent.getBeanName();
			return useFlowIdAsPrefix
					? prefix + beanName
					: beanName;
		}
		else if (fallbackId != null) {
			return useFlowIdAsPrefix
					? prefix + fallbackId
					: fallbackId;
		}

		String generatedBeanName = prefix;
		if (instance instanceof NamedComponent namedComponent) {
			generatedBeanName += namedComponent.getComponentType();
		}
		else {
			generatedBeanName += instance.getClass().getName();
		}

		String id = generatedBeanName;
		int counter = -1;
		while (counter == -1 || this.beanFactory.containsBean(id)) {
			counter++;
			id = generatedBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
		}
		return id;
	}

}
