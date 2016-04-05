/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.MessagingException;


/**
 * A gateway proxy factory that can handle JDK8 {@link CompletableFuture}s. If a
 * gateway method returns {@link CompletableFuture} exactly, one will be returned and the
 * results of the
 * {@link CompletableFuture#supplyAsync(Supplier, java.util.concurrent.Executor)} method
 * call will be returned.
 * <p>
 * If you wish your integration flow to return a {@link CompletableFuture} to the gateway
 * in a reply message, the async executor must be set to {@code null}.
 * <p>
 * If the return type is a subclass of {@link CompletableFuture},
 * it must be returned by the integration flow and the async executor (if present) is not
 * used.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class GatewayCompletableFutureProxyFactoryBean extends GatewayProxyFactoryBean {

	public GatewayCompletableFutureProxyFactoryBean() {
		super();
	}

	public GatewayCompletableFutureProxyFactoryBean(Class<?> serviceInterface) {
		super(serviceInterface);
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		final Class<?> returnType = invocation.getMethod().getReturnType();
		if (CompletableFuture.class.equals(returnType)) { // exact
			AsyncTaskExecutor asyncExecutor = getAsyncExecutor();
			if (asyncExecutor != null) {
				return CompletableFuture.supplyAsync(new Invoker(invocation), asyncExecutor);
			}
		}
		return super.invoke(invocation);
	}

	private final class Invoker implements Supplier<Object> {

		private final MethodInvocation invocation;

		private Invoker(MethodInvocation methodInvocation) {
			this.invocation = methodInvocation;
		}

		@Override
		public Object get() {
			try {
				return doInvoke(this.invocation, false);
			}
			catch (Error e) { //NOSONAR
				throw e;
			}
			catch (Throwable t) { //NOSONAR
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				}
				throw new MessagingException("asynchronous gateway invocation failed", t);
			}
		}

	}

}
