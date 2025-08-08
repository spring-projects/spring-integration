/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router;

import java.lang.reflect.Method;

import org.springframework.integration.annotation.Router;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;

/**
 * A Message Router that invokes the specified method on the given object. The
 * method's return value may be a single MessageChannel instance, a single
 * String to be interpreted as a channel name, or a Collection (or Array) of
 * either type. If the method returns channel names, then a
 * {@link org.springframework.messaging.core.DestinationResolver} is required.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingRouter extends AbstractMessageProcessingRouter {

	public MethodInvokingRouter(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Object>(object, method));
	}

	public MethodInvokingRouter(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Object>(object, methodName));
	}

	public MethodInvokingRouter(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<?>) object :
				new MethodInvokingMessageProcessor<Object>(object, Router.class));
	}

}
