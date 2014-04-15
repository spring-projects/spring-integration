/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanPostProcessor} implementation that processes method-level
 * messaging annotations such as @Transformer, @Splitter, @Router, and @Filter.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class MessagingAnnotationPostProcessor implements BeanPostProcessor, BeanFactoryAware,
		InitializingBean, Lifecycle, ApplicationListener<ApplicationEvent>, EnvironmentAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ConfigurableListableBeanFactory beanFactory;

	private Environment environment;

	private final Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> postProcessors =
			new HashMap<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>>();

	private final Set<ApplicationListener<ApplicationEvent>> listeners = new HashSet<ApplicationListener<ApplicationEvent>>();

	private final Set<Lifecycle> lifecycles = new HashSet<Lifecycle>();

	private volatile boolean running = true;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isAssignable(ConfigurableListableBeanFactory.class, beanFactory.getClass(),
				"a ConfigurableListableBeanFactory is required");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		postProcessors.put(Filter.class, new FilterAnnotationPostProcessor(this.beanFactory, this.environment));
		postProcessors.put(Router.class, new RouterAnnotationPostProcessor(this.beanFactory, this.environment));
		postProcessors.put(Transformer.class, new TransformerAnnotationPostProcessor(this.beanFactory, this.environment));
		postProcessors.put(ServiceActivator.class, new ServiceActivatorAnnotationPostProcessor(this.beanFactory, this.environment));
		postProcessors.put(Splitter.class, new SplitterAnnotationPostProcessor(this.beanFactory, this.environment));
		postProcessors.put(Aggregator.class, new AggregatorAnnotationPostProcessor(this.beanFactory, this.environment));
		postProcessors.put(InboundChannelAdapter.class, new InboundChannelAdapterAnnotationPostProcessor(this.beanFactory, this.environment));
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		final Class<?> beanClass = this.getBeanClass(bean);
		if (!this.isStereotype(beanClass)) {
			// we only post-process stereotype components
			return bean;
		}
		ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				List<Annotation> annotations = new ArrayList<Annotation>();
				for (Class<? extends Annotation> annotation : postProcessors.keySet()) {
					Annotation result = AnnotationUtils.getAnnotation(method, annotation);
					if (result != null) {
						annotations.add(result);
					}
				}
				for (Annotation annotation : annotations) {
					MethodAnnotationPostProcessor postProcessor = postProcessors.get(annotation.annotationType());
					if (postProcessor != null && shouldCreateEndpoint(annotation)) {
						Object result = postProcessor.postProcess(bean, beanName, method, annotation);
						if (result != null && result instanceof AbstractEndpoint) {
							AbstractEndpoint endpoint = (AbstractEndpoint) result;
							String autoStartup = (String) AnnotationUtils.getValue(annotation, "autoStartup");
							if (StringUtils.hasText(autoStartup)) {
								if (environment != null) {
									autoStartup = environment.resolvePlaceholders(autoStartup);
								}
								if (StringUtils.hasText(autoStartup)) {
									endpoint.setAutoStartup(Boolean.parseBoolean(autoStartup));
								}
							}

							String phase = (String) AnnotationUtils.getValue(annotation, "phase");
							if (StringUtils.hasText(phase)) {
								if (environment != null) {
									phase = environment.resolvePlaceholders(phase);
								}
								if (StringUtils.hasText(phase)) {
									endpoint.setPhase(Integer.parseInt(phase));
								}
							}

							String endpointBeanName = generateBeanName(beanName, method, annotation.annotationType());
							endpoint.setBeanName(endpointBeanName);
							beanFactory.registerSingleton(endpointBeanName, endpoint);
							endpoint.setBeanFactory(beanFactory);
							try {
								endpoint.afterPropertiesSet();
							}
							catch (Exception e) {
								throw new BeanInitializationException("failed to initialize annotated component", e);
							}
							lifecycles.add(endpoint);
							if (endpoint.isAutoStartup()) {
								endpoint.start();
							}
							if (result instanceof ApplicationListener) {
								listeners.add((ApplicationListener) result);
							}
						}
					}
				}
			}
		});
		return bean;
	}

	private boolean shouldCreateEndpoint(Annotation annotation) {
		Object inputChannel = AnnotationUtils.getValue(annotation, "inputChannel");
		if (inputChannel == null && annotation instanceof InboundChannelAdapter) {
			inputChannel = AnnotationUtils.getValue(annotation);
		}
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


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		for (ApplicationListener<ApplicationEvent> listener : listeners) {
			try  {
				listener.onApplicationEvent(event);
			}
			catch (ClassCastException e) {
				if (logger.isWarnEnabled() && event != null) {
					logger.warn("ApplicationEvent of type [" + event.getClass() +
							"] not accepted by ApplicationListener [" + listener + "]");
				}
			}
		}
	}

	// Lifecycle implementation

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void start() {
		for (Lifecycle lifecycle : this.lifecycles) {
			if (!lifecycle.isRunning()) {
				lifecycle.start();
			}
		}
		this.running = true;
	}

	@Override
	public void stop() {
		for (Lifecycle lifecycle : this.lifecycles) {
			if (lifecycle.isRunning()) {
				lifecycle.stop();
			}
		}
		this.running = false;
	}

}
