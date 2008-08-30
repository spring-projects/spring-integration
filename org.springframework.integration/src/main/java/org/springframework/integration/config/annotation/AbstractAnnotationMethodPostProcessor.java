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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.bus.MessageBus;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Base class for post-processing annotated methods.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractAnnotationMethodPostProcessor<T> implements AnnotationMethodPostProcessor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Class<? extends Annotation> annotationType;

	private final MessageBus messageBus;

	private final ClassLoader beanClassLoader;


	public AbstractAnnotationMethodPostProcessor(Class<? extends Annotation> annotationType, MessageBus messageBus, ClassLoader beanClassLoader) {
		Assert.notNull(annotationType, "Annotation type must not be null.");
		Assert.notNull(messageBus, "MessageBus must not be null.");
		this.annotationType = annotationType;
		this.messageBus = messageBus;
		this.beanClassLoader = (beanClassLoader != null) ? beanClassLoader : ClassUtils.getDefaultClassLoader();
	}


	protected MessageBus getMessageBus() {
		return this.messageBus;
	}

	public Object postProcess(final Object bean, final String beanName, final Class<?> originalBeanClass) {
		final List<T> results = new ArrayList<T>();
		ReflectionUtils.doWithMethods(originalBeanClass, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = getAnnotation(method);
				if (annotation != null) {
					T result = processMethod(bean, method, annotation);
					if (result != null) {
						results.add(result);
					}
				}
			}
		});
		T postProcessedBean = (results.size() > 0) ? this.processResults(results) : null;
		if (postProcessedBean == null) {
			return bean;
		}
		ProxyFactory proxyFactory = new ProxyFactory(bean);
		proxyFactory.addAdvice(new DelegatingIntroductionInterceptor(postProcessedBean));
		return proxyFactory.getProxy(this.beanClassLoader);
	}

	private Annotation getAnnotation(Method method) {
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		for (Annotation annotation : annotations) {
			if (annotation.annotationType().equals(this.annotationType)
					|| annotation.annotationType().isAnnotationPresent(this.annotationType)) {
				return annotation;
			}
		}
		return null;
	}


	protected abstract T processMethod(Object bean, Method method, Annotation annotation);

	protected abstract T processResults(List<T> results);

}
