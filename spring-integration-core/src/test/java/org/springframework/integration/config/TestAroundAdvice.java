/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.CountDownLatch;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Mark Fisher
 */
public class TestAroundAdvice implements MethodInterceptor {

	private final CountDownLatch latch;

	private volatile long preCount;

	private volatile long postCount;

	public TestAroundAdvice(CountDownLatch latch) {
		this.latch = latch;
	}

	public long getPreCount() {
		return this.preCount;
	}

	public long getPostCount() {
		return this.postCount;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		try {
			this.preCount = this.latch.getCount();
			this.latch.countDown();
			return invocation.proceed();
		}
		finally {
			this.postCount = this.latch.getCount();
			this.latch.countDown();
		}
	}

}
