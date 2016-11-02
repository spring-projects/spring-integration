/*
 * Copyright 2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
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
 * @since 5.0
 */
public class IntegrationFlowBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware,
		SmartInitializingSingleton {

	private final Set<ApplicationListener<?>> applicationListeners = new HashSet<ApplicationListener<?>>();

	private ConfigurableListableBeanFactory beanFactory;

	private AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"To use Spring Integration Java DSL the 'beanFactory' has to be an instance of " +
						"'ConfigurableListableBeanFactory'. Consider using 'GenericApplicationContext' implementation."
		);

		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
		this.autowiredAnnotationBeanPostProcessor.setBeanFactory(this.beanFactory);
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
		if (this.beanFactory.containsBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			ApplicationEventMulticaster multicaster =
					(ApplicationEventMulticaster) this.beanFactory.getBean(
							AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
			this.applicationListeners.forEach(multicaster::addApplicationListener);
		}

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

	private Object processStandardIntegrationFlow(StandardIntegrationFlow flow, String beanName) {
		String flowNamePrefix = beanName + ".";
		int subFlowNameIndex = 0;
		int channelNameIndex = 0;
		boolean registerSingleton = flow.isRegisterComponents();


		List<Object> integrationComponents = new ArrayList<>(flow.getIntegrationComponents());
		for (int i = 0; i < integrationComponents.size(); i++) {
			Object component = integrationComponents.get(i);
			if (component instanceof ConsumerEndpointSpec) {
				ConsumerEndpointSpec<?, ?> endpointSpec = (ConsumerEndpointSpec<?, ?>) component;
				MessageHandler messageHandler = endpointSpec.get().getT2();
				ConsumerEndpointFactoryBean endpoint = endpointSpec.get().getT1();
				String id = endpointSpec.getId();

				Collection<?> messageHandlers = this.beanFactory.getBeansOfType(messageHandler.getClass(), false,
						false).values();

				if (!messageHandlers.contains(messageHandler)) {
					String handlerBeanName = generateBeanName(messageHandler);
					String[] handlerAlias = id != null
							? new String[] { id + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX }
							: null;

					registerComponent(messageHandler, handlerBeanName, beanName, registerSingleton);
					if (handlerAlias != null) {
						for (String alias : handlerAlias) {
							this.beanFactory.registerAlias(handlerBeanName, alias);
						}
					}
				}

				String endpointBeanName = id;
				if (endpointBeanName == null) {
					endpointBeanName = generateBeanName(endpoint);
				}
				registerComponent(endpoint, endpointBeanName, beanName, registerSingleton);
				integrationComponents.set(i, endpoint);
			}
			else {
				Collection<?> values = this.beanFactory.getBeansOfType(component.getClass(), false, false).values();
				if (!values.contains(component)) {
					if (component instanceof AbstractMessageChannel) {
						String channelBeanName = ((AbstractMessageChannel) component).getComponentName();
						if (channelBeanName == null) {
							channelBeanName = flowNamePrefix + "channel" +
									BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + channelNameIndex++;
						}
						registerComponent(component, channelBeanName, beanName, registerSingleton);
					}
					else if (component instanceof MessageChannelReference) {
						String channelBeanName = ((MessageChannelReference) component).getName();
						if (!this.beanFactory.containsBean(channelBeanName)) {
							DirectChannel directChannel = new DirectChannel();
							registerComponent(directChannel, channelBeanName, beanName, registerSingleton);
							integrationComponents.set(i, directChannel);
						}
					}
					else if (component instanceof FixedSubscriberChannel) {
						FixedSubscriberChannel fixedSubscriberChannel = (FixedSubscriberChannel) component;
						String channelBeanName = fixedSubscriberChannel.getComponentName();
						if ("Unnamed fixed subscriber channel".equals(channelBeanName)) {
							channelBeanName = flowNamePrefix + "channel" +
									BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + channelNameIndex++;
						}
						registerComponent(component, channelBeanName, beanName, registerSingleton);
					}
					else if (component instanceof SourcePollingChannelAdapterSpec) {
						SourcePollingChannelAdapterSpec spec = (SourcePollingChannelAdapterSpec) component;
						Collection<Object> componentsToRegister = spec.getComponentsToRegister();
						if (!CollectionUtils.isEmpty(componentsToRegister)) {
							componentsToRegister.stream()
									.filter(o -> !this.beanFactory.getBeansOfType(o.getClass(), false, false)
											.values()
											.contains(o))
									.forEach(o -> registerComponent(o, generateBeanName(o))
									);
						}
						SourcePollingChannelAdapterFactoryBean pollingChannelAdapterFactoryBean = spec.get().getT1();
						String id = spec.getId();
						if (!StringUtils.hasText(id)) {
							id = generateBeanName(pollingChannelAdapterFactoryBean);
						}
						registerComponent(pollingChannelAdapterFactoryBean, id, beanName, registerSingleton);
						integrationComponents.set(i, pollingChannelAdapterFactoryBean);

						MessageSource<?> messageSource = spec.get().getT2();
						if (!this.beanFactory.getBeansOfType(messageSource.getClass(), false, false)
								.values()
								.contains(messageSource)) {
							String messageSourceId = id + ".source";
							if (messageSource instanceof NamedComponent
									&& ((NamedComponent) messageSource).getComponentName() != null) {
								messageSourceId = ((NamedComponent) messageSource).getComponentName();
							}
							registerComponent(messageSource, messageSourceId, beanName, registerSingleton);
						}
					}
					else if (component instanceof StandardIntegrationFlow) {
						String subFlowBeanName = flowNamePrefix + "subFlow" +
								BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + subFlowNameIndex++;
						registerComponent(component, subFlowBeanName, beanName, registerSingleton);
					}
					else {
						String generateBeanName = generateBeanName(component);
						registerComponent(component, generateBeanName, beanName, registerSingleton);
					}
				}
			}
		}
		flow.setIntegrationComponents(integrationComponents);
		return flow;
	}

	private Object processIntegrationFlowImpl(IntegrationFlow flow, String beanName) {
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(beanName + ".input");
		flow.configure(flowBuilder);
		Object standardIntegrationFlow = processStandardIntegrationFlow(flowBuilder.get(), beanName);
		return isLambda(flow) ? standardIntegrationFlow : flow;
	}

	private void processIntegrationComponentSpec(IntegrationComponentSpec<?, ?> bean) {
		registerComponent(bean.get(), generateBeanName(bean.get()), null, false);
		if (bean instanceof ComponentsRegistration) {
			Collection<Object> componentsToRegister = ((ComponentsRegistration) bean).getComponentsToRegister();
			if (!CollectionUtils.isEmpty(componentsToRegister)) {
				componentsToRegister.stream()
						.filter(component -> !this.beanFactory.getBeansOfType(component.getClass(), false, false)
								.values()
								.contains(component))
						.forEach(component -> registerComponent(component, generateBeanName(component)));
			}
		}
	}

	private void registerComponent(Object component, String beanName) {
		registerComponent(component, beanName, null, true);
	}

	private void registerComponent(Object component, String beanName, String parentName, boolean registerSingleton) {
		if (component instanceof ApplicationListener) {
			this.applicationListeners.add((ApplicationListener<?>) component);
		}
		this.autowiredAnnotationBeanPostProcessor.processInjection(component);
		this.beanFactory.initializeBean(component, beanName);
		if (registerSingleton) {
			this.beanFactory.registerSingleton(beanName, component);
			if (parentName != null) {
				this.beanFactory.registerDependentBean(parentName, beanName);
			}
		}
	}

	private String generateBeanName(Object instance) {
		if (instance instanceof NamedComponent && ((NamedComponent) instance).getComponentName() != null) {
			return ((NamedComponent) instance).getComponentName();
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
