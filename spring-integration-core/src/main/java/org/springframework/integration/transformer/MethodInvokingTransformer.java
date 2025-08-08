/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.lang.reflect.Method;

import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.util.Assert;

/**
 * A Message Transformer implementation that invokes the specified method
 * on the given object. The method's return value will be considered as
 * the payload of a new Message unless the return value is itself already
 * a Message.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingTransformer extends AbstractMessageProcessingTransformer {

	public MethodInvokingTransformer(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Object>(object, method));
		Assert.state(!Void.class.isAssignableFrom(method.getReturnType()), "'transformer' method must not be 'void'.");
	}

	public MethodInvokingTransformer(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Object>(object, methodName));
	}

	public MethodInvokingTransformer(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<?>) object :
				new MethodInvokingMessageProcessor<Object>(object, Transformer.class));
	}

}
