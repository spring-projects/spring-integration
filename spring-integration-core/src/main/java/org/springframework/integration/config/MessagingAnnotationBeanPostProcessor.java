/*
 * Copyright 2023-present the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.Role;
import org.springframework.integration.config.annotation.MethodAnnotationPostProcessor;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An infrastructure {@link BeanPostProcessor} implementation that processes method-level
 * messaging annotations such as @Transformer, @Splitter, @Router, and @Filter.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class MessagingAnnotationBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {

	private final Set<Class<?>> noAnnotationsCache = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	private final Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> postProcessors;

	private final List<Runnable> methodsToPostProcessAfterContextInitialization = new ArrayList<>();

	@SuppressWarnings("NullAway.Init")
	private ConfigurableListableBeanFactory beanFactory;

	private volatile boolean initialized;

	public MessagingAnnotationBeanPostProcessor(
			Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> postProcessors) {

		this.postProcessors = postProcessors;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");

		Class<?> beanClass = AopUtils.getTargetClass(bean);

		// the set will hold records of prior class scans and indicate if no messaging annotations were found
		if (this.noAnnotationsCache.contains(beanClass)) {
			return bean;
		}

		ReflectionUtils.doWithMethods(beanClass,
				method -> doWithMethod(method, bean, beanName, beanClass),
				ReflectionUtils.USER_DECLARED_METHODS);

		return bean;
	}

	private void doWithMethod(Method method, Object bean, String beanName, Class<?> beanClass) {
		MergedAnnotations mergedAnnotations = MergedAnnotations.from(method);

		boolean noMessagingAnnotations = true;

		// See MessagingAnnotationPostProcessor.postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)
		if (!mergedAnnotations.isPresent(Bean.class)) {
			List<MessagingMetaAnnotation> messagingAnnotations =
					obtainMessagingAnnotations(this.postProcessors.keySet(), mergedAnnotations,
							method.toGenericString());

			for (MessagingMetaAnnotation messagingAnnotation : messagingAnnotations) {
				noMessagingAnnotations = false;
				Class<? extends Annotation> annotationType = messagingAnnotation.annotationType;
				List<Annotation> annotationChain =
						MessagingAnnotationUtils.getAnnotationChain(messagingAnnotation.annotation, annotationType);
				processAnnotationTypeOnMethod(bean, beanName, method, annotationType, annotationChain);
			}
		}
		if (noMessagingAnnotations) {
			this.noAnnotationsCache.add(beanClass);
		}
	}

	private void processAnnotationTypeOnMethod(Object bean, String beanName, Method method,
			Class<? extends Annotation> annotationType, List<Annotation> annotations) {

		MethodAnnotationPostProcessor<?> postProcessor = this.postProcessors.get(annotationType);
		if (postProcessor != null && postProcessor.supportsPojoMethod()
				&& postProcessor.shouldCreateEndpoint(method, annotations)) {

			Method targetMethod = method;
			if (AopUtils.isJdkDynamicProxy(bean)) {
				try {
					targetMethod = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
				}
				catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("Service methods must be extracted to the service "
							+ "interface for JdkDynamicProxy. The affected bean is: '" + beanName + "' "
							+ "and its method: '" + method + "'", e);
				}
			}

			if (this.initialized) {
				postProcessMethodAndRegisterEndpointIfAny(bean, beanName, method, annotationType, annotations,
						postProcessor, targetMethod);
			}
			else {
				Method methodToPostProcess = targetMethod;
				this.methodsToPostProcessAfterContextInitialization.add(() ->
						postProcessMethodAndRegisterEndpointIfAny(bean, beanName, method, annotationType, annotations,
								postProcessor, methodToPostProcess));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void postProcessMethodAndRegisterEndpointIfAny(Object bean, String beanName, Method method,
			Class<? extends Annotation> annotationType, List<Annotation> annotations,
			MethodAnnotationPostProcessor<?> postProcessor, Method targetMethod) {

		Object result = postProcessor.postProcess(bean, beanName, targetMethod, annotations);
		if (result instanceof AbstractEndpoint endpoint) {
			String autoStartup = MessagingAnnotationUtils.resolveAttribute(annotations, "autoStartup", String.class);
			if (StringUtils.hasText(autoStartup)) {
				autoStartup = this.beanFactory.resolveEmbeddedValue(autoStartup);
				if (StringUtils.hasText(autoStartup)) {
					endpoint.setAutoStartup(Boolean.parseBoolean(autoStartup));
				}
			}

			String phase = MessagingAnnotationUtils.resolveAttribute(annotations, "phase", String.class);
			if (StringUtils.hasText(phase)) {
				phase = this.beanFactory.resolveEmbeddedValue(phase);
				if (StringUtils.hasText(phase)) {
					endpoint.setPhase(Integer.parseInt(phase));
				}
			}

			Role role = AnnotationUtils.findAnnotation(method, Role.class);
			if (role != null) {
				endpoint.setRole(role.value());
			}

			String endpointBeanName = generateBeanName(beanName, method, annotationType);
			endpoint.setBeanName(endpointBeanName);
			((BeanDefinitionRegistry) this.beanFactory).registerBeanDefinition(endpointBeanName,
					new RootBeanDefinition((Class<AbstractEndpoint>) endpoint.getClass(), () -> endpoint));
			this.beanFactory.getBean(endpointBeanName);
		}
	}

	protected String generateBeanName(String originalBeanName, Method method,
			Class<? extends Annotation> annotationType) {

		String name = MessagingAnnotationUtils.endpointIdValue(method);
		if (!StringUtils.hasText(name)) {
			String baseName = originalBeanName + "." + method.getName() + "."
					+ ClassUtils.getShortNameAsProperty(annotationType);
			name = baseName;
			int count = 1;
			while (this.beanFactory.containsBean(name)) {
				name = baseName + "#" + (++count);
			}
		}
		return name;
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.initialized = true;
		this.methodsToPostProcessAfterContextInitialization.forEach(Runnable::run);
		this.methodsToPostProcessAfterContextInitialization.clear();
	}

	protected static List<MessagingMetaAnnotation> obtainMessagingAnnotations(
			Set<Class<? extends Annotation>> postProcessors, MergedAnnotations annotations, String identified) {

		List<MessagingMetaAnnotation> messagingAnnotations = new ArrayList<>();

		for (Class<? extends Annotation> annotationType : postProcessors) {
			annotations.stream()
					.filter((ann) -> ann.getType().equals(annotationType))
					.map(MergedAnnotation::getRoot)
					.map(MergedAnnotation::synthesize)
					.map((ann) -> new MessagingMetaAnnotation(ann, annotationType))
					.forEach(messagingAnnotations::add);
		}

		if (annotations.get(EndpointId.class, (ann) -> ann.hasNonDefaultValue("value")).isPresent()
				&& messagingAnnotations.size() > 1) {

			throw new IllegalStateException("@EndpointId on " + identified
					+ " can only have one EIP annotation, found: " + messagingAnnotations.size());
		}

		return messagingAnnotations;
	}

	public record MessagingMetaAnnotation(Annotation annotation, Class<? extends Annotation> annotationType) {

	}

}
