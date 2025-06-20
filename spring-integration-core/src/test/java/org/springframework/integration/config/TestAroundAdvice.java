/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
