/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * The base {@link HandleMessageAdvice} for advices which can be applied only
 * for the {@link MessageHandler#handleMessage(Message)}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.3.1
 */
public abstract class AbstractHandleMessageAdvice extends IntegrationObjectSupport implements HandleMessageAdvice {

	@Override
	public final Object invoke(MethodInvocation invocation) throws Throwable { // NOSONAR
		Method method = invocation.getMethod();
		Object invocationThis = invocation.getThis();
		Object[] arguments = invocation.getArguments();
		boolean isMessageHandler = invocationThis instanceof MessageHandler;
		boolean isMessageMethod = method.getName().equals("handleMessage")
				&& (arguments.length == 1 && arguments[0] instanceof Message);
		if (!isMessageHandler || !isMessageMethod) {
			if (this.logger.isWarnEnabled()) {
				String clazzName = invocationThis == null
						? method.getDeclaringClass().getName()
						: invocationThis.getClass().getName();
				this.logger.warn("This advice " + getClass().getName() +
						" can only be used for MessageHandlers; an attempt to advise method '"
						+ method.getName() + "' in '" + clazzName + "' is ignored.");
			}
			return invocation.proceed();
		}

		Message<?> message = (Message<?>) arguments[0];
		return doInvoke(invocation, message);
	}

	protected abstract Object doInvoke(MethodInvocation invocation, Message<?> message) throws Throwable; // NOSONAR

}
