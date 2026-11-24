/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import org.springframework.aop.AfterReturningAdvice;

/**
 * @author Mark Fisher
 */
public class TestAfterAdvice implements AfterReturningAdvice {

	private final CountDownLatch latch;

	private volatile long latchCount;

	public TestAfterAdvice(CountDownLatch latch) {
		this.latch = latch;
	}

	public long getLatchCount() {
		return this.latchCount;
	}

	public void afterReturning(Object returnValue, Method method,
			Object[] args, Object target) throws Throwable {
		this.latchCount = this.latch.getCount();
		this.latch.countDown();
	}

}
