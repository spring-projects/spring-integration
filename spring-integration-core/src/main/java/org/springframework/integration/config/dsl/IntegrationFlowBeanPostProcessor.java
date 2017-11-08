/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.config.dsl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.ConsumerEndpointSpec;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.dsl.IntegrationFlow;
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

/**
 * A {@link BeanPostProcessor} to parse {@link IntegrationFlow} beans and
 * register their components as beans in the provided {@link BeanFactory},
 * if necessary.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class IntegrationFlowBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"To use Spring Integration Java DSL the 'beanFactory' has to be an instance of " +
						"'ConfigurableListableBeanFactory'. Consider using 'GenericApplicationContext' implementation."
		);

		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
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
			processIntegrationComponentSpec((IntegrationComponentSpec<?, ?>) bean);
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
							"Consider to use [IntegrationFlowContext] for manual registration of IntegrationFlows.");
				}
			}
		}
	}

	private Object processStandardIntegrationFlow(StandardIntegrationFlow flow, String flowBeanName) {
		String flowNamePrefix = flowBeanName + ".";
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
					id = generateBeanName(endpoint, entry.getValue());
				}

				Collection<?> messageHandlers =
						this.beanFactory.getBeansOfType(messageHandler.getClass(), false, false)
								.values();

				if (!messageHandlers.contains(messageHandler)) {
					String handlerBeanName = generateBeanName(messageHandler);
					String[] handlerAlias = new String[] { id + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX };

					registerComponent(messageHandler, handlerBeanName, flowBeanName);
					for (String alias : handlerAlias) {
						this.beanFactory.registerAlias(handlerBeanName, alias);
					}
				}

				registerComponent(endpoint, id, flowBeanName);
				targetIntegrationComponents.put(endpoint, id);
			}
			else {
				Collection<?> values = this.beanFactory.getBeansOfType(component.getClass(), false, false).values();
				if (!values.contains(component)) {
					if (component instanceof AbstractMessageChannel) {
						String channelBeanName = ((AbstractMessageChannel) component).getComponentName();
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
									.filter(o ->
											!this.beanFactory.getBeansOfType(o.getKey().getClass(), false, false)
													.values()
													.contains(o.getKey()))
									.forEach(o ->
											registerComponent(o.getKey(), generateBeanName(o.getKey(), o.getValue())));
						}
						SourcePollingChannelAdapterFactoryBean pollingChannelAdapterFactoryBean = spec.get().getT1();
						String id = spec.getId();
						if (!StringUtils.hasText(id)) {
							id = generateBeanName(pollingChannelAdapterFactoryBean, entry.getValue());
						}
						registerComponent(pollingChannelAdapterFactoryBean, id, flowBeanName);
						targetIntegrationComponents.put(pollingChannelAdapterFactoryBean, id);

						MessageSource<?> messageSource = spec.get().getT2();
						if (!this.beanFactory.getBeansOfType(messageSource.getClass(), false, false)
								.values()
								.contains(messageSource)) {
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
											.setSource(new DescriptiveResource(gateway.getObjectType().getName()));
								});

						targetIntegrationComponents.put(component, gatewayId);
					}
					else {
						String generatedBeanName = generateBeanName(component, entry.getValue());
						registerComponent(component, generatedBeanName, flowBeanName);
						targetIntegrationComponents.put(component, generatedBeanName);
					}
				}
				else {
					targetIntegrationComponents.put(entry.getKey(), entry.getValue());
				}
			}
		}

		flow.setIntegrationComponents(targetIntegrationComponents);
		return flow;
	}

	private Object processIntegrationFlowImpl(IntegrationFlow flow, String beanName) {
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(beanName + ".input");
		flow.configure(flowBuilder);
		Object standardIntegrationFlow = processStandardIntegrationFlow(flowBuilder.get(), beanName);
		return isLambda(flow) ? standardIntegrationFlow : flow;
	}

	private void processIntegrationComponentSpec(IntegrationComponentSpec<?, ?> bean) {
		registerComponent(bean.get(), generateBeanName(bean.get(), bean.getId()));
		if (bean instanceof ComponentsRegistration) {
			Map<Object, String> componentsToRegister = ((ComponentsRegistration) bean).getComponentsToRegister();
			if (!CollectionUtils.isEmpty(componentsToRegister)) {

				componentsToRegister.entrySet()
						.stream()
						.filter(component ->
								!this.beanFactory.getBeansOfType(component.getKey().getClass(), false, false)
										.values()
										.contains(component.getKey()))
						.forEach(component ->
								registerComponent(component.getKey(),
										generateBeanName(component.getKey(), component.getValue())));

			}
		}
	}

	private void registerComponent(Object component, String beanName) {
		registerComponent(component, beanName, null);
	}

	@SuppressWarnings("unchecked")
	private void registerComponent(Object component, String beanName, String parentName,
			BeanDefinitionCustomizer... customizers) {

		BeanDefinition beanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) component.getClass(), () -> component)
						.applyCustomizers(customizers)
						.getRawBeanDefinition();

		((BeanDefinitionRegistry) this.beanFactory).registerBeanDefinition(beanName, beanDefinition);

		if (parentName != null) {
			this.beanFactory.registerDependentBean(parentName, beanName);
		}

		this.beanFactory.getBean(beanName);
	}

	private String generateBeanName(Object instance) {
		return generateBeanName(instance, null);
	}

	private String generateBeanName(Object instance, String fallbackId) {
		if (instance instanceof NamedComponent && ((NamedComponent) instance).getComponentName() != null) {
			return ((NamedComponent) instance).getComponentName();
		}
		else if (fallbackId != null) {
			return fallbackId;
		}

		String generatedBeanName = instance.getClass().getName();
		String id = generatedBeanName;
		int counter = -1;
		while (counter == -1 || this.beanFactory.containsBean(id)) {
			counter++;
			id = generatedBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
		}
		return id;
	}

	private static boolean isLambda(Object o) {
		Class<?> aClass = o.getClass();
		return aClass.isSynthetic() && !aClass.isAnonymousClass() && !aClass.isLocalClass();
	}

}
