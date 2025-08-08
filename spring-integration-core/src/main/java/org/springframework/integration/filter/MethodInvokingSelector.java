/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import java.lang.reflect.Method;

import org.springframework.integration.annotation.Filter;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.util.Assert;

/**
 * A method-invoking implementation of
 * {@link org.springframework.integration.core.MessageSelector}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class MethodInvokingSelector extends AbstractMessageProcessingSelector {

	public MethodInvokingSelector(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Boolean>(object, method));
		Class<?> returnType = method.getReturnType();
		Assert.isTrue(boolean.class.isAssignableFrom(returnType)
						|| Boolean.class.isAssignableFrom(returnType),
				"MethodInvokingSelector method must return a boolean result.");
	}

	public MethodInvokingSelector(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Boolean>(object, methodName));
	}

	@SuppressWarnings("unchecked")
	public MethodInvokingSelector(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<Boolean>) object :
				new MethodInvokingMessageProcessor<Boolean>(object, Filter.class));
	}

}
