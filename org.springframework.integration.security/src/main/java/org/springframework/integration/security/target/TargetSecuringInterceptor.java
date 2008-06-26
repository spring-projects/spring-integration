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

package org.springframework.integration.security.target;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.integration.message.Message;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * 
 * @author Jonas Partner
 *
 */
public class TargetSecuringInterceptor implements MethodInterceptor {

	private final ConfigAttributeDefinition targetSecurityAttributes;

	private final AccessDecisionManager accessDecisionManager;

	public TargetSecuringInterceptor(ConfigAttributeDefinition targetSecurityAttributes,
			AccessDecisionManager accessDecisionManager) {
		this.targetSecurityAttributes = targetSecurityAttributes;
		this.accessDecisionManager = accessDecisionManager;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		Message<?> message = (Message<?>) invocation.getArguments()[0];
		SecurityContext ctx = SecurityContextUtils.getSecurityContextFromHeader(message);
		if (ctx != null) {
			try {
				SecurityContextHolder.setContext(ctx);
				accessDecisionManager.decide(SecurityContextHolder.getContext().getAuthentication(), invocation
						.getThis(), targetSecurityAttributes);
				return invocation.proceed();
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		}
		else {
			accessDecisionManager.decide(SecurityContextHolder.getContext().getAuthentication(), invocation.getThis(),
					targetSecurityAttributes);
			return invocation.proceed();
		}
	}

}
