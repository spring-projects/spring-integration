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

package org.springframework.integration.endpoint;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class EndpointMethodInterceptor implements MethodInterceptor {

	private final EndpointInterceptor interceptor;


	public EndpointMethodInterceptor(EndpointInterceptor interceptor) {
		Assert.notNull(interceptor, "EndpointInterceptor must not be null.");
		this.interceptor = interceptor;
	}


	public Object invoke(MethodInvocation invocation) throws Throwable {
		Message<?> message = null;
		try {
			message = (Message<?>) invocation.getArguments()[0];
		}
		catch (Exception e) {
			throw new IllegalStateException("EndpointMethodInterceptor is only applicable for the "
					+ "'MessageEndpoint.invoke(Message message)' method.");
		}
		if (!this.interceptor.preInvoke(message)) {
			return Boolean.FALSE;
		}
		boolean returnValue = this.interceptor.aroundInvoke(invocation);
		this.interceptor.postInvoke(message, returnValue);
		return returnValue;
	}

}
