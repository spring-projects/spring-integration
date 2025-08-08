/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
