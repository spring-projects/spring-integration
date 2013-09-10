/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.Repeat;

/**
 * A method rule that looks at Spring repeat annotations on methods and executes the test multiple times (without
 * re-initializing the test case).
 * 
 * @author Dave Syer
 * @since 2.0
 * 
 */
public class RepeatProcessor implements MethodRule {

	private final int concurrency;

	public RepeatProcessor(int concurrency) {
		this.concurrency = concurrency < 0 ? 0 : concurrency;
	}

	public Statement apply(final Statement base, FrameworkMethod method, Object target) {
		Repeat repeat = AnnotationUtils.findAnnotation(method.getMethod(), Repeat.class);
		if (repeat == null) {
			return base;
		}
		final int repeats = repeat.value();
		if (concurrency <= 0) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					for (int i = 0; i < repeats; i++) {
						try {
							base.evaluate();
						} catch (Throwable t) {
							throw new IllegalStateException("Failed on iteration: " + i, t);
						}
					}
				}
			};
		}
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
				ExecutorService executor = Executors.newFixedThreadPool(concurrency);
				try {
					for (int i = 0; i < repeats; i++) {
						final int count = i;
						results.add(executor.submit(new Callable<Boolean>() {
							public Boolean call() {
								try {
									base.evaluate();
								} catch (Throwable t) {
									throw new IllegalStateException("Failed on iteration: " + count, t);
								}
								return true;
							}
						}));
					}
					for (Future<Boolean> future : results) {
						assertTrue("Null result from completer", future.get(10, TimeUnit.SECONDS));
					}
				} finally {
					executor.shutdownNow();
				}
			}
		};
	}
}
