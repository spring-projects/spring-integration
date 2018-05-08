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

package org.springframework.integration.dsl;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.context.SmartLifecycle;
import org.springframework.util.ObjectUtils;

/**
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
