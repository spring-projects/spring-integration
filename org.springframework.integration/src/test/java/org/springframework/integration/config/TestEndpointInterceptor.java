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

package org.springframework.integration.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.endpoint.interceptor.EndpointInterceptorAdapter;

/**
 * @author Mark Fisher
 */
public class TestEndpointInterceptor extends EndpointInterceptorAdapter {

	private AtomicInteger counter = new AtomicInteger();


	public int getCount() {
		return this.counter.get();
	}

	public boolean aroundInvoke(MethodInvocation invocation) throws Throwable {
		this.counter.incrementAndGet();
		Boolean result = (Boolean) invocation.proceed();
		this.counter.incrementAndGet();
		return result;
	}

}
