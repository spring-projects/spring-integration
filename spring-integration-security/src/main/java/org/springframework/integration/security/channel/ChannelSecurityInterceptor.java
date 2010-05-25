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

package org.springframework.integration.security.channel;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.security.intercept.AbstractSecurityInterceptor;
import org.springframework.security.intercept.InterceptorStatusToken;
import org.springframework.security.intercept.ObjectDefinitionSource;
import org.springframework.util.Assert;

/**
 * An AOP interceptor that enforces authorization for MessageChannel send and/or receive calls.
 * 
 * @author Mark Fisher
 */
public class ChannelSecurityInterceptor extends AbstractSecurityInterceptor implements MethodInterceptor {

	private final ChannelInvocationDefinitionSource objectDefinitionSource;


	public ChannelSecurityInterceptor(ChannelInvocationDefinitionSource objectDefinitionSource) {
		Assert.notNull(objectDefinitionSource, "objectDefinitionSource must not be null");
		this.objectDefinitionSource = objectDefinitionSource;
	}


	@Override
	public Class<?> getSecureObjectClass() {
		return ChannelInvocation.class;
	}

	@Override
	public ObjectDefinitionSource obtainObjectDefinitionSource() {
		return this.objectDefinitionSource;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (method.getName().equals("send") || method.getName().equals("receive")) {
			return this.invokeWithAuthorizationCheck(invocation);
		}
		return invocation.proceed();
	}

	private Object invokeWithAuthorizationCheck(MethodInvocation methodInvocation) throws Throwable {
		Object returnValue = null;
		InterceptorStatusToken token = super.beforeInvocation(new ChannelInvocation(methodInvocation));
		try {
			returnValue = methodInvocation.proceed();
		}
		finally {
			returnValue = super.afterInvocation(token, returnValue);
		}
		return returnValue;
	}

}
