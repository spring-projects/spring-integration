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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNull;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.integration.security.config.SecurityTestUtil;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class SecurityEndpointInterceptorTests {

	@Test(expected = AccessDeniedException.class)
	public void testUnauthenticatedAccessToSecuredEndpointWithNullMessage() throws Throwable {
		try {
			Object target = new Object();
			MethodInvocation invocation = createTestMethodInvocationWithNullMessage(target);
			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");
			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(null, target, attDefintion);
			expectLastCall().andThrow(new AccessDeniedException("nope"));

			replay(invocation);
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.aroundInvoke(invocation);

			verify(invocation, adm);
		}
		finally {
			assertNull("Authentication was not null after invocation threw AccessDeniedException",
					SecurityContextHolder.getContext().getAuthentication());
		}
	}

	@Test(expected = AccessDeniedException.class)
	public void testUnauthenticatedAccessToSecuredEndpoint() throws Throwable {
		try {
			Object target = new Object();
			MethodInvocation invocation = createTestMethodInvocationNoSecurityHeaderInMessage(target);
			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");
			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(null, target, attDefintion);
			expectLastCall().andThrow(new AccessDeniedException("nope"));

			replay(invocation);
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.aroundInvoke(invocation);

			verify(invocation, adm);
		}
		finally {
			assertNull("Authentication was not null after invocation threw AccessDeniedException",
					SecurityContextHolder.getContext().getAuthentication());
		}
	}

	@Test
	public void testAuthenticatedAccessToSecuredEndpoint() throws Throwable {
		try {
			Object target = new Object();
			SecurityContext context = SecurityTestUtil.createContext("bob", "bobspassword",
					new String[] { "ROLE_ADMIN" });
			MethodInvocation invocation = createTestMethodInvocation(target, context);
			expect(invocation.proceed()).andReturn(null);
			replay(invocation);

			ConfigAttributeDefinition attDefintion = new ConfigAttributeDefinition("ROLE_ADMIN");

			AccessDecisionManager adm = createMock(AccessDecisionManager.class);
			adm.decide(context.getAuthentication(), target, attDefintion);
			expectLastCall();
			replay(adm);

			SecurityEndpointInterceptor interceptor = new SecurityEndpointInterceptor(attDefintion, adm);
			interceptor.aroundInvoke(invocation);

			verify(invocation, adm);

		}
		finally {
			assertNull("Authentication was not null after successful invocation", SecurityContextHolder.getContext()
					.getAuthentication());
		}
	}

	public MethodInvocation createTestMethodInvocation(Object target, SecurityContext securityContext) {
		Message message = new StringMessage("test");
		SecurityContextUtils.setSecurityContextHeader(securityContext, message);
		MethodInvocation mockInvocation = createMock(MethodInvocation.class);
		expect(mockInvocation.getArguments()).andReturn(new Object[] { message });
		expect(mockInvocation.getThis()).andReturn(target);
		return mockInvocation;
	}

	public MethodInvocation createTestMethodInvocationNoSecurityHeaderInMessage(Object target) {
		Message message = new StringMessage("test");
		MethodInvocation mockInvocation = createMock(MethodInvocation.class);
		expect(mockInvocation.getArguments()).andReturn(new Object[] { message });
		expect(mockInvocation.getThis()).andReturn(target);
		return mockInvocation;
	}

	public MethodInvocation createTestMethodInvocationWithNullMessage(Object target) {
		MethodInvocation mockInvocation = createMock(MethodInvocation.class);
		expect(mockInvocation.getArguments()).andReturn(new Object[] { null });
		expect(mockInvocation.getThis()).andReturn(target);
		return mockInvocation;
	}

}
