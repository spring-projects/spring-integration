/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.dsl.context;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.util.ObjectUtils;

/**
 * An AOP {@link MethodInterceptor} for the {@link IntegrationFlow} proxies
 * with a delegation to an associated {@link StandardIntegrationFlow} instance, which
 * is not exposed as a bean during a target {@link IntegrationFlow} bean processing.
 *
 * <p> In most cases an associated internal {@link StandardIntegrationFlow}
 * exposes an {@code inputChannel} bean for the target {@link IntegrationFlow},
 * which doesn't start from the channel, e.g. instantiated from lambda.
 * This way the advice first tries to obtain an {@code inputChannel} from the
 * target {@link IntegrationFlow} and then falls back to the {@link #delegate}.
 *
 * <p> Another aspect of this advice is to control and delegate {@link SmartLifecycle}
 * of the target {@link IntegrationFlow} and associated {@link #delegate}.
 * The {@link SmartLifecycle#start()} and {@link SmartLifecycle#stop()} operations
 * are delegated to the {@link StandardIntegrationFlow} as is because that instance
 * isn't controlled by the standard application context lifecycle.
 * The {@link SmartLifecycle#isAutoStartup()}, {@link SmartLifecycle#getPhase()}
 * and {@link SmartLifecycle#isRunning()} are called on the {@link #delegate}
 * only in case when {@link MethodInvocation#proceed()} returns {@code null}
 * or isn't called at all, e.g. when target {@link IntegrationFlow} doesn't
 * implement {@link SmartLifecycle}.
 *
 * @author Artem Bilan
 *
 * @since 5.1
 */
class IntegrationFlowLifecycleAdvice implements MethodInterceptor {

	private final StandardIntegrationFlow delegate;

	IntegrationFlowLifecycleAdvice(StandardIntegrationFlow delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object target = invocation.getThis();
		String method = invocation.getMethod().getName();
		Object result = null;

		if ("getInputChannel".equals(method)) {
			result = invocation.proceed();
			if (result == null) {
				result = this.delegate.getInputChannel();
			}
		}
		else {
			if (target instanceof SmartLifecycle) {
				result = invocation.proceed();
			}

			switch (method) {

				case "start":
					this.delegate.start();
					break;

				case "stop":
					Object[] arguments = invocation.getArguments();
					if (!ObjectUtils.isEmpty(arguments)) {
						this.delegate.stop((Runnable) arguments[0]);
					}
					else {
						this.delegate.stop();
					}
					break;

				case "isRunning":
					if (result == null) {
						result = this.delegate.isRunning();
					}
					break;

				case "isAutoStartup":
					if (result == null) {
						result = this.delegate.isAutoStartup();
					}
					break;

				case "getPhase":
					if (result == null) {
						result = this.delegate.getPhase();
					}
					break;
			}
		}

		return result;
	}

}
