/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.util.ErrorHandler;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class RunnableProxyingMethodInterceptorTests {

	TaskExecutor proxiedExecutor;

	StubErrorHandler errorHandler;

	@Before
	public void setUp() {
		errorHandler = new StubErrorHandler();
		ProxyFactory factory = new ProxyFactory(new SimpleAsyncTaskExecutor());
		factory.addAdvice(new RunnableProxyingMethodInterceptor(errorHandler));
		proxiedExecutor = (TaskExecutor) factory.getProxy();
	}

	@Test
	public void testRuntimeThrown() throws Exception {
		errorHandler.latch = new CountDownLatch(1);
		TestRunnable runnable = new TestRunnable();
		proxiedExecutor.execute(runnable);
		assertTrue("Runnable did not run", errorHandler.latch.await(5, TimeUnit.SECONDS));
		assertEquals("Wrong count of exceptions in ErrorHandler", 1, errorHandler.throwables.size());
	}

	public static class StubErrorHandler implements ErrorHandler {

		CountDownLatch latch;

		List<Throwable> throwables = new ArrayList<Throwable>();

		public void handle(Throwable t) {
			throwables.add(t);
			latch.countDown();
		}
	}

	public static class TestRunnable implements Runnable {

		public void run() {
			throw new RuntimeException();
		}
	}

}
