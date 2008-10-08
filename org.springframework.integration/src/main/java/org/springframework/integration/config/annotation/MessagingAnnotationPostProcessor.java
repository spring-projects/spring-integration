/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanPostProcessor} implementation that processes method-level
 * messaging annotations such as @Transformer, @Splitter, and @Router.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessagingAnnotationPostProcessor implements BeanPostProcessor, BeanFactoryAware, InitializingBean {

	private volatile MessageBus messageBus;

	private volatile ConfigurableBeanFactory beanFactory;


	private final Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> postProcessors =
			new HashMap<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>>();


	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isAssignable(ConfigurableBeanFactory.class, beanFactory.getClass(),
				"a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		this.messageBus = (MessageBus) this.beanFactory.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		postProcessors.put(Aggregator.class, new AggregatorAnnotationPostProcessor(this.beanFactory));
		postProcessors.put(ChannelAdapter.class, new ChannelAdapterAnnotationPostProcessor(this.beanFactory));
		postProcessors.put(Router.class, new RouterAnnotationPostProcessor(this.beanFactory));
		postProcessors.put(ServiceActivator.class, new ServiceActivatorAnnotationPostProcessor(this.beanFactory));
		postProcessors.put(Splitter.class, new SplitterAnnotationPostProcessor(this.beanFactory));
		postProcessors.put(Transformer.class, new TransformerAnnotationPostProcessor(this.beanFactory));
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		final Class<?> beanClass = this.getBeanClass(bean);
		if (!this.isStereotype(beanClass)) {
			// we only post-process stereotype components
			return bean;
		}
		ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
			@SuppressWarnings("unchecked")
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation[] annotations = AnnotationUtils.getAnnotations(method);
				for (Annotation annotation : annotations) {
					MethodAnnotationPostProcessor postProcessor = postProcessors.get(annotation.annotationType());
					if (postProcessor != null) {
						if (!shouldCreateEndpoint(annotation)) {
							continue;
						}
						Object result = postProcessor.postProcess(bean, beanName, method, annotation);
						if (result != null && result instanceof MessageEndpoint) {
							String endpointBeanName = generateBeanName(beanName, method, annotation.annotationType());
							if (result instanceof BeanNameAware) {
								((BeanNameAware) result).setBeanName(endpointBeanName);
							}
							beanFactory.registerSingleton(endpointBeanName, result);
						}
					}
				}
			}
		});
		if (bean instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) bean).setChannelRegistry(messageBus);
		}
		return bean;
	}

	private boolean shouldCreateEndpoint(Annotation annotation) {
		if (annotation instanceof ChannelAdapter) {
			return true;
		}
		Object inputChannel = AnnotationUtils.getValue(annotation, "inputChannel");
		return (inputChannel != null && inputChannel instanceof String
				&& StringUtils.hasText((String) inputChannel));
	}

	private Class<?> getBeanClass(Object bean) {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		return (targetClass != null) ? targetClass : bean.getClass();
	}

	private boolean isStereotype(Class<?> beanClass) {
		List<Annotation> annotations = new ArrayList<Annotation>(Arrays.asList(beanClass.getAnnotations()));
		Class<?>[] interfaces = beanClass.getInterfaces();
		for (Class<?> iface : interfaces) {
			annotations.addAll(Arrays.asList(iface.getAnnotations()));
		}
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> annotationType = annotation.annotationType();
			if (annotationType.equals(Component.class) || annotationType.isAnnotationPresent(Component.class)) {
				return true;
			}
		}
		return false;
	}

	private String generateBeanName(String originalBeanName, Method method, Class<? extends Annotation> annotationType) {
		String baseName = originalBeanName + "." + method.getName() + "." + ClassUtils.getShortNameAsProperty(annotationType);
		String name = baseName;
		int count = 1;
		while (this.beanFactory.containsBean(name)) {
			name = baseName + "#" + (++count);
		}
		return name;
	}

}
