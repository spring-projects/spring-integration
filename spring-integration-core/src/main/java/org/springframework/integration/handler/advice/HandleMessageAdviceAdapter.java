/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.util.Assert;

/**
 * A {@link HandleMessageAdvice} implementation with a plain delegation
 * to the provided {@link MethodInterceptor}.
 * <p> This advice should be used for consumer endpoints to proxy exactly
 * a {@link org.springframework.messaging.MessageHandler#handleMessage} and the whole-subflow therefore;
 * unlike regular proxying which is applied only for the
 * {@link org.springframework.integration.handler.AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class HandleMessageAdviceAdapter implements HandleMessageAdvice {

	private final MethodInterceptor delegate;

	public HandleMessageAdviceAdapter(MethodInterceptor delegate) {
		Assert.notNull(delegate, "The 'delegate' must not be null");
		this.delegate = delegate;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		return this.delegate.invoke(invocation);
	}

}
