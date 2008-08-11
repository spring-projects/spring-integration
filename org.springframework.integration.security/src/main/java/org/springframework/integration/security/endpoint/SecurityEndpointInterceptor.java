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

package org.springframework.integration.security.endpoint;

import org.springframework.integration.endpoint.interceptor.EndpointInterceptorAdapter;
import org.springframework.integration.message.Message;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * @author Jonas Partner
 */
public class SecurityEndpointInterceptor extends EndpointInterceptorAdapter {

	private final ConfigAttributeDefinition targetSecurityAttributes;

	private final AccessDecisionManager accessDecisionManager;

	public SecurityEndpointInterceptor(ConfigAttributeDefinition endpointSecurityAttributes,
			AccessDecisionManager accessDecisionManager) {
		super();
		this.targetSecurityAttributes = endpointSecurityAttributes;
		this.accessDecisionManager = accessDecisionManager;
	}

	@Override
	public Message<?> preHandle(Message<?> message) {
		SecurityContext securityContext = null;
		if (message != null) {
			securityContext = SecurityContextUtils.getSecurityContextFromHeader(message);
		}
		if (securityContext != null) {
			try {
				SecurityContextHolder.setContext(securityContext);
				this.accessDecisionManager.decide(SecurityContextHolder.getContext().getAuthentication(),
						message, this.targetSecurityAttributes);
				return message;
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		}
		else {
			this.accessDecisionManager.decide(SecurityContextHolder.getContext().getAuthentication(),
					message, this.targetSecurityAttributes);
			return message;
		}
	}

}
