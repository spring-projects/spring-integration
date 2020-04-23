/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.ConsumerEndpointSpec;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.gateway.AnnotationGatewayProxyFactoryBean;
import org.springframework.integration.support.context.NamedComponent;
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
		implements BeanPostProcessor, ApplicationContextAware, SmartInitializingSingleton {

	private ConfigurableApplicationContext applicationContext;

	private StringValueResolver embeddedValueResolver;

	private ConfigurableListableBeanFactory beanFactory;

	private volatile IntegrationFlowContext flowContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
				"To use Spring Integration Java DSL the 'applicationContext' has to be an instance of " +
						"'ConfigurableApplicationContext'. Consider using 'GenericApplicationContext' implementation."
		);

		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
		this.beanFactory = this.applicationContext.getBeanFactory();
		this.embeddedValueResolver = new EmbeddedValueResolver(this.beanFactory);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof StandardIntegrationFlow) {
			return processStandardIntegrationFlow((StandardIntegrationFlow) bean, beanName);
		}
		else if (bean instanceof IntegrationFlow) {
			return processIntegrationFlowImpl((IntegrationFlow) bean, beanName);
		}
		if (bean instanceof IntegrationComponentSpec) {
			processIntegrationComponentSpec(beanName, (IntegrationComponentSpec<?, ?>) bean);
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

	private Object processStandardIntegrationFlow(StandardIntegrationFlow flow, String flowBeanName) { // NOSONAR
		// complexity
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
			if (component instanceof ConsumerEndpointSpec) {
				ConsumerEndpointSpec<?, ?> endpointSpec = (ConsumerEndpointSpec<?, ?>) component;
				MessageHandler messageHandler = endpointSpec.get().getT2();
				ConsumerEndpointFactoryBean endpoint = endpointSpec.get().getT1();
				String id = endpointSpec.getId();

				if (id == null) {
					id = generateBeanName(endpoint, flowNamePrefix, entry.getValue(), useFlowIdAsPrefix);
				}
				else if (useFlowIdAsPrefix) {
					id = flowNamePrefix + id;
				}

				if (noBeanPresentForComponent(messageHandler, flowBeanName)) {
					String handlerBeanName = generateBeanName(messageHandler, flowNamePrefix);

					registerComponent(messageHandler, handlerBeanName, flowBeanName);
					this.beanFactory.registerAlias(handlerBeanName, id + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX);
				}

				registerComponent(endpoint, id, flowBeanName);
				targetIntegrationComponents.put(endpoint, id);
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
						registerComponent(component, channelBeanName, flowBeanName);
						targetIntegrationComponents.put(component, channelBeanName);
					}
					else if (component instanceof MessageChannelReference) {
						String channelBeanName = ((MessageChannelReference) component).getName();
						if (!this.beanFactory.containsBean(channelBeanName)) {
							DirectChannel directChannel = new DirectChannel();
							registerComponent(directChannel, channelBeanName, flowBeanName);
							targetIntegrationComponents.put(directChannel, channelBeanName);
						}
					}
					else if (component instanceof FixedSubscriberChannel) {
						FixedSubscriberChannel fixedSubscriberChannel = (FixedSubscriberChannel) component;
						String channelBeanName = fixedSubscriberChannel.getComponentName();
						if ("Unnamed fixed subscriber channel".equals(channelBeanName)) {
							channelBeanName = flowNamePrefix + "channel" +
									BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + channelNameIndex++;
						}
						registerComponent(component, channelBeanName, flowBeanName);
						targetIntegrationComponents.put(component, channelBeanName);
					}
					else if (component instanceof SourcePollingChannelAdapterSpec) {
						SourcePollingChannelAdapterSpec spec = (SourcePollingChannelAdapterSpec) component;
						Map<Object, String> componentsToRegister = spec.getComponentsToRegister();
						if (!CollectionUtils.isEmpty(componentsToRegister)) {
							componentsToRegister.entrySet()
									.stream()
									.filter(o -> noBeanPresentForComponent(o.getKey(), flowBeanName))
									.forEach(o ->
											registerComponent(o.getKey(),
													generateBeanName(o.getKey(), flowNamePrefix, o.getValue(),
															useFlowIdAsPrefix)));
						}
						SourcePollingChannelAdapterFactoryBean pollingChannelAdapterFactoryBean = spec.get().getT1();
						String id = spec.getId();
						if (id == null) {
							id = generateBeanName(pollingChannelAdapterFactoryBean, flowNamePrefix, entry.getValue(),
									useFlowIdAsPrefix);
						}
						else if (useFlowIdAsPrefix) {
							id = flowNamePrefix + id;
						}

						registerComponent(pollingChannelAdapterFactoryBean, id, flowBeanName);
						targetIntegrationComponents.put(pollingChannelAdapterFactoryBean, id);

						MessageSource<?> messageSource = spec.get().getT2();
						if (noBeanPresentForComponent(messageSource, flowBeanName)) {
							String messageSourceId = id + ".source";
							if (messageSource instanceof NamedComponent
									&& ((NamedComponent) messageSource).getComponentName() != null) {
								messageSourceId = ((NamedComponent) messageSource).getComponentName();
							}
							registerComponent(messageSource, messageSourceId, flowBeanName);
						}
					}
					else if (component instanceof StandardIntegrationFlow) {
						String subFlowBeanName =
								entry.getValue() != null
										? entry.getValue()
										: flowNamePrefix + "subFlow" +
												BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + subFlowNameIndex++;
						registerComponent(component, subFlowBeanName, flowBeanName);
						targetIntegrationComponents.put(component, subFlowBeanName);
					}
					else if (component instanceof AnnotationGatewayProxyFactoryBean) {
						AnnotationGatewayProxyFactoryBean gateway = (AnnotationGatewayProxyFactoryBean) component;
						String gatewayId = entry.getValue();

						if (gatewayId == null) {
							gatewayId = gateway.getComponentName();
						}
						if (gatewayId == null) {
							gatewayId = flowNamePrefix + "gateway";
						}

						registerComponent(gateway, gatewayId, flowBeanName,
								beanDefinition -> {
									((AbstractBeanDefinition) beanDefinition)
											.setSource(new DescriptiveResource("" + gateway.getObjectType()));
								});

						targetIntegrationComponents.put(component, gatewayId);
					}
					else {
						String generatedBeanName =
								generateBeanName(component, flowNamePrefix, entry.getValue(), useFlowIdAsPrefix);

						registerComponent(component, generatedBeanName, flowBeanName);
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
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(beanName + ".input");

		flow.configure(flowBuilder);

		StandardIntegrationFlow target = flowBuilder.get();
		processStandardIntegrationFlow(target, beanName);

		if (!(flow instanceof IntegrationFlowAdapter)) {
			NameMatchMethodPointcutAdvisor integrationFlowAdvice =
					new NameMatchMethodPointcutAdvisor(new IntegrationFlowLifecycleAdvice(target));
			integrationFlowAdvice.setMappedNames(
					"getInputChannel",
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
		Object target = bean.get();

		invokeBeanInitializationHooks(beanName, target);

		if (bean instanceof ComponentsRegistration) {
			Map<Object, String> componentsToRegister = ((ComponentsRegistration) bean).getComponentsToRegister();
			if (!CollectionUtils.isEmpty(componentsToRegister)) {

				componentsToRegister.entrySet()
						.stream()
						.filter(component -> noBeanPresentForComponent(component.getKey(), beanName))
						.forEach(component ->
								registerComponent(component.getKey(),
										generateBeanName(component.getKey(), component.getValue())));

			}
		}
	}

	private void invokeBeanInitializationHooks(final String beanName, final Object bean) { // NOSONAR complexity
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware && this.beanFactory.getBeanClassLoader() != null) {
				((BeanClassLoaderAware) bean).setBeanClassLoader(this.beanFactory.getBeanClassLoader()); // NOSONAR
			}
			if (bean instanceof BeanFactoryAware) {
				((BeanFactoryAware) bean).setBeanFactory(this.beanFactory);
			}
			if (bean instanceof EnvironmentAware) {
				((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
			}
			if (bean instanceof EmbeddedValueResolverAware) {
				((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
			}
			if (bean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
			}
			if (bean instanceof ApplicationEventPublisherAware) {
				((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
			}
			if (bean instanceof MessageSourceAware) {
				((MessageSourceAware) bean).setMessageSource(this.applicationContext);
			}
			if (bean instanceof ApplicationContextAware) {
				((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean noBeanPresentForComponent(Object instance, String parentBeanName) {
		if (instance instanceof NamedComponent) {
			String beanName = ((NamedComponent) instance).getBeanName();
			if (beanName != null) {
				if (this.beanFactory.containsBean(beanName)) {
					BeanDefinition existingBeanDefinition =
							IntegrationContextUtils.getBeanDefinition(beanName, this.beanFactory);
					if (!ConfigurableBeanFactory.SCOPE_PROTOTYPE.equals(existingBeanDefinition.getScope())
							&& !instance.equals(this.beanFactory.getBean(beanName))) {

						AbstractBeanDefinition beanDefinition =
								BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) instance.getClass(),
										() -> instance)
										.getBeanDefinition();
						beanDefinition.setResourceDescription("the '" + parentBeanName + "' bean definition");
						throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingBeanDefinition);
					}
					else {
						return false;
					}
				}
				else {
					return true;
				}
			}
		}

		return !this.beanFactory.getBeansOfType(instance.getClass(), false, false)
				.values()
				.contains(instance);
	}

	private void registerComponent(Object component, String beanName) {
		registerComponent(component, beanName, null);
	}

	@SuppressWarnings("unchecked")
	private void registerComponent(Object component, String beanName, String parentName,
			BeanDefinitionCustomizer... customizers) {

		AbstractBeanDefinition beanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) component.getClass(), () -> component)
						.applyCustomizers(customizers)
						.getRawBeanDefinition();

		if (parentName != null && this.beanFactory.containsBeanDefinition(parentName)) {
			AbstractBeanDefinition parentBeanDefinition =
					(AbstractBeanDefinition) this.beanFactory.getBeanDefinition(parentName);
			beanDefinition.setResource(parentBeanDefinition.getResource());
			Object source = parentBeanDefinition.getSource();
			if (source instanceof MethodMetadata) {
				source = "bean method " + ((MethodMetadata) source).getMethodName();
			}
			beanDefinition.setSource(source);
			this.beanFactory.registerDependentBean(parentName, beanName);
		}

		((BeanDefinitionRegistry) this.beanFactory).registerBeanDefinition(beanName, beanDefinition);


		this.beanFactory.getBean(beanName);
	}

	private String generateBeanName(Object instance, String prefix) {
		return generateBeanName(instance, prefix, null, false);
	}

	private String generateBeanName(Object instance, String prefix, String fallbackId, boolean useFlowIdAsPrefix) {
		if (instance instanceof NamedComponent && ((NamedComponent) instance).getBeanName() != null) {
			String beanName = ((NamedComponent) instance).getBeanName();
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
		if (instance instanceof NamedComponent) {
			generatedBeanName += ((NamedComponent) instance).getComponentType();
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
