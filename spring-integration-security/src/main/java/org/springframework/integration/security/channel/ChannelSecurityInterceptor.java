/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.util.Assert;

/**
 * An AOP interceptor that enforces authorization for MessageChannel send and/or receive calls.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public final class ChannelSecurityInterceptor extends AbstractSecurityInterceptor implements MethodInterceptor {

	private final ChannelSecurityMetadataSource securityMetadataSource;


	public ChannelSecurityInterceptor(ChannelSecurityMetadataSource securityMetadataSource) {
		Assert.notNull(securityMetadataSource, "securityMetadataSource must not be null");
		this.securityMetadataSource = securityMetadataSource;
	}


	@Override
	public Class<?> getSecureObjectClass() {
		return ChannelInvocation.class;
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


	@Override
	public SecurityMetadataSource obtainSecurityMetadataSource() {
		return this.securityMetadataSource;
	}

}
