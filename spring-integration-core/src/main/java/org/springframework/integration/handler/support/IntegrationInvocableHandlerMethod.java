/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.handler.support;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.core.CoroutinesUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * An {@link InvocableHandlerMethod} extension for Spring Integration requirements.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class IntegrationInvocableHandlerMethod extends InvocableHandlerMethod {

	public IntegrationInvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	@Override
	protected @Nullable Object doInvoke(@Nullable Object... args) throws Exception {
		Method method = getBridgedMethod();
		if (KotlinDetector.isSuspendingFunction(method)) {
			return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
		}
		else {
			return super.doInvoke(args);
		}
	}

}
