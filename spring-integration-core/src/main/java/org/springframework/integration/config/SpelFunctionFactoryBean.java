/*
 * Copyright 2013-present the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A {@link FactoryBean} implementation to encapsulate the population of a static {@link Method}
 * from the provided {@linkplain #functionClass} and {@linkplain #functionMethodSignature} as
 * a valid {@link org.springframework.expression.spel.support.StandardEvaluationContext} function.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 * @see org.springframework.expression.spel.support.StandardEvaluationContext#registerFunction
 * @see BeanUtils#resolveSignature
 */
public class SpelFunctionFactoryBean implements FactoryBean<Method>, InitializingBean, BeanNameAware {

	private final Class<?> functionClass;

	private final String functionMethodSignature;

	private @Nullable String functionName;

	private @Nullable Method method;

	public SpelFunctionFactoryBean(Class<?> functionClass, String functionMethodSignature) {
		this.functionClass = functionClass;
		this.functionMethodSignature = functionMethodSignature;
	}

	@Override
	public void setBeanName(String name) {
		this.functionName = name;
	}

	public @Nullable String getFunctionName() {
		return this.functionName;
	}

	@Override
	public void afterPropertiesSet() {
		this.method = BeanUtils.resolveSignature(this.functionMethodSignature, this.functionClass);

		if (this.method == null) {
			throw new BeanDefinitionStoreException(String.format("No declared method '%s' in class '%s'",
					this.functionMethodSignature, this.functionClass));
		}
		if (!Modifier.isStatic(this.method.getModifiers())) {
			throw new BeanDefinitionStoreException("SpEL-function method has to be 'static'");
		}
	}

	@Override
	public @Nullable Method getObject() {
		return this.method;
	}

	@Override
	public Class<?> getObjectType() {
		return Method.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
