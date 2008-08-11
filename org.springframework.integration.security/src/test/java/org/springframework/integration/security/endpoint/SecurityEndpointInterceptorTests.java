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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.integration.security.SecurityTestUtil;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class SecurityEndpointInterceptorTests {

	@Test(expected = AccessDeniedException.class)
	public void testUnauthenticatedAccessToSecuredEndpointWithNullMessage() throws Throwable {
		try {
			Message<?> message = null;
			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");
			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(null, message, attDefintion);
			expectLastCall().andThrow(new AccessDeniedException("nope"));
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.preHandle(message);
			verify(adm);
		}
		finally {
			assertNull("Authentication was not null after invocation threw AccessDeniedException",
					SecurityContextHolder.getContext().getAuthentication());
		}
	}

	@Test(expected = AccessDeniedException.class)
	public void testUnauthenticatedAccessToSecuredEndpointWithNoSecurityContext() throws Throwable {
		try {
			Message<?> message = this.createMessageWithoutContext();
			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");
			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(null, message, attDefintion);
			expectLastCall().andThrow(new AccessDeniedException("nope"));
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.preHandle(message);
			verify(adm);
		}
		finally {
			assertNull("Authentication was not null after invocation threw AccessDeniedException",
					SecurityContextHolder.getContext().getAuthentication());
		}
	}

	@Test(expected = AccessDeniedException.class)
	public void testUnauthenticatedAccessToSecuredEndpointWithSecurityContext() throws Throwable {
		try {
			SecurityContext context = SecurityTestUtil.createContext("bob", "bobspassword",
					new String[] { "ROLE_ADMIN" });
			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");
			Message<?> message = this.createMessageWithContext(context);

			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(context.getAuthentication(), message, attDefintion);
			expectLastCall().andThrow(new AccessDeniedException("nope"));
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.preHandle(message);
			verify(adm);
		}
		finally {
			assertNull("Authentication was not null after successful invocation", SecurityContextHolder.getContext()
					.getAuthentication());
		}
	}

	@Test
	public void testAuthenticatedAccessToSecuredEndpoint() throws Throwable {
		try {
			SecurityContext context = SecurityTestUtil.createContext("bob", "bobspassword",
					new String[] { "ROLE_ADMIN" });
			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");
			Message<?> message = this.createMessageWithContext(context);
			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(context.getAuthentication(), message, attDefintion);
			expectLastCall();
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.preHandle(message);
			verify(adm);
		}
		finally {
			assertNull("Authentication was not null after successful invocation", SecurityContextHolder.getContext()
					.getAuthentication());
		}
	}

	public Message<?> createMessageWithContext(SecurityContext securityContext) {
		Message<?> message = new StringMessage("test");
		return SecurityContextUtils.setSecurityContextHeader(securityContext, message);
	}

	public Message<?> createMessageWithoutContext() {
		return new StringMessage("test");
	}

}
