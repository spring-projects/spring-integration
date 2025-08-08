/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.splitter;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;

/**
 * A Message Splitter implementation that invokes the specified method
 * on the given object. The method's return value will be split if it
 * is a Collection or Array. If the return value is not a Collection or
 * Array, then the single Object will be returned as the payload of a
 * single reply Message.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingSplitter extends AbstractMessageProcessingSplitter {

	public MethodInvokingSplitter(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Collection<?>>(object, method));
	}

	public MethodInvokingSplitter(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Collection<?>>(object, methodName));
	}

	@SuppressWarnings("unchecked")
	public MethodInvokingSplitter(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<Collection<?>>) object :
				new MethodInvokingMessageProcessor<Collection<?>>(object, Splitter.class));
	}

}
