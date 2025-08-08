/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

	private String functionName;

	private Method method;

	public SpelFunctionFactoryBean(Class<?> functionClass, String functionMethodSignature) {
		this.functionClass = functionClass;
		this.functionMethodSignature = functionMethodSignature;
	}

	@Override
	public void setBeanName(String name) {
		this.functionName = name;
	}

	public String getFunctionName() {
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
	public Method getObject() {
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
