/*
 * Copyright 2013 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.annotation.SpelFunction;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class SpelFunctionAnnotationPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, final String beanName) throws BeansException {
		final Class<?> beanClass = bean.getClass();
		ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				SpelFunction spelFunctionAnnotation = method.getAnnotation(SpelFunction.class);
				if (spelFunctionAnnotation != null) {
					if (Modifier.isStatic(method.getModifiers())) {
						IntegrationEvaluationContextFactoryBean evaluationContextFactoryBean =
								SpelFunctionAnnotationPostProcessor.this.beanFactory.getBean(IntegrationEvaluationContextFactoryBean.class);
						String functionName = spelFunctionAnnotation.value();
						if (!StringUtils.hasText(functionName)) {
							functionName = method.getName();
						}
						evaluationContextFactoryBean.addFunction(functionName, method);
					}
					else {
						throw new BeanCreationNotAllowedException(beanName, "'@SpelFunction' is allowed only on static methods.");
					}
				}
			}
		});
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
