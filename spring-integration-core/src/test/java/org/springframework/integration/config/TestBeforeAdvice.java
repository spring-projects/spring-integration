/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import org.springframework.aop.MethodBeforeAdvice;

/**
 * @author Mark Fisher
 */
public class TestBeforeAdvice implements MethodBeforeAdvice {

	private final CountDownLatch latch;

	private volatile long latchCount;

	public TestBeforeAdvice(CountDownLatch latch) {
		this.latch = latch;
	}

	public long getLatchCount() {
		return this.latchCount;
	}

	public void before(Method method, Object[] args, Object target) throws Throwable {
		this.latchCount = latch.getCount();
		latch.countDown();
	}

}
