/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import java.util.concurrent.locks.ReentrantLock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Dave Syer
 *
 */
public class LockInterceptor extends ReentrantLock implements MethodInterceptor {

	private static final long serialVersionUID = 1L;

	public synchronized Object invoke(MethodInvocation invocation) throws Throwable {
		return invocation.proceed();
	}

}
