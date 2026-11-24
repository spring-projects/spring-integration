/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
