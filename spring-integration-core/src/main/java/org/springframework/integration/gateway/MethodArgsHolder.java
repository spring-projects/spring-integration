/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.gateway;

import java.lang.reflect.Method;

/**
 * Simple wrapper class containing a {@link Method} and an object
 * array containing the arguments for an invocation of that method.
 * For example used by a {@link MethodArgsMessageMapper} with this generic
 * type to provide custom argument mapping when creating a message
 * in a {@code GatewayProxyFactoryBean}.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public final class MethodArgsHolder {

	private final Method method;

	private final Object[] args;

	public MethodArgsHolder(Method method, Object[] args) { //NOSONAR - direct storage
		this.method = method;
		this.args = args; //NOSONAR - direct storage
	}

	public Method getMethod() {
		return this.method;
	}

	public Object[] getArgs() {
		return this.args; //NOSONAR - direct access
	}

}
