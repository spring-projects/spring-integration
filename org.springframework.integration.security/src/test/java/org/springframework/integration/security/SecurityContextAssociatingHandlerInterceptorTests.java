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

package org.springframework.integration.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * @author Jonas Partner
 */
public class SecurityContextAssociatingHandlerInterceptorTests {

	@After
	public void clearSecurityContext(){
		SecurityContextHolder.clearContext();
	}
	
	@Test
	public void testMessageWithSecurityContext() {
		final StubSecurityContext securityContext = new StubSecurityContext();
		StringMessage message = new StringMessage("test");
		message.getHeader().setAttribute(
				SecurityContextPropagatingChannelInterceptor.SECURITY_CONTEXT_HEADER_ATTRIBUTE, securityContext);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				SecurityContext associatedContext = SecurityContextHolder.getContext();
				assertEquals("Wrong security context", securityContext, associatedContext);
				return null;
			}
		};
		SecurityContextAssociatingHandlerInterceptor associatingInterceptor =
				new SecurityContextAssociatingHandlerInterceptor(handler);
		associatingInterceptor.handle(message);
		assertNull("Security context still present after handler returned",
				SecurityContextHolder.getContext().getAuthentication());
	}

	@Test(expected = AccessDeniedException.class)
	public void testForSecurityLeakageIfHandlerThrowsException() {
		final StubSecurityContext securityContext = new StubSecurityContext();
		StringMessage message = new StringMessage("test");
		message.getHeader().setAttribute(
				SecurityContextPropagatingChannelInterceptor.SECURITY_CONTEXT_HEADER_ATTRIBUTE, securityContext);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				SecurityContext associatedContext = SecurityContextHolder.getContext();
				assertEquals("Wrong security context", securityContext, associatedContext);
				throw new AccessDeniedException("Not allowed");
			}
		};
		SecurityContextAssociatingHandlerInterceptor associatingInterceptor = 
				new SecurityContextAssociatingHandlerInterceptor(handler);
		try {
			associatingInterceptor.handle(message);
		}
		finally {
			assertNull("Security context still present after handler threw exception",
					SecurityContextHolder.getContext().getAuthentication());
		}
	}

	@Test
	public void testMessageWithoutSecurityContext() {
		final StubSecurityContext securityContext = new StubSecurityContext();
		StringMessage message = new StringMessage("test");
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				SecurityContext associatedContext = SecurityContextHolder.getContext();
				assertNotSame("Wrong security context", securityContext, associatedContext);
				return null;
			}
		};
		SecurityContextAssociatingHandlerInterceptor associatingInterceptor =
				new SecurityContextAssociatingHandlerInterceptor(handler);
		associatingInterceptor.handle(message);
		assertNull("Security context still present after handler returned",
				SecurityContextHolder.getContext().getAuthentication());
	}
	
	@Test
	public void testExistingSecurityContextIsNotCleared(){
		SecurityContextHolder.setStrategyName(StackBasedSecurityContextHolderStrategy.class.getName());
		final StubSecurityContext securityContext = new StubSecurityContext();
		SecurityContextHolder.setContext(securityContext);
		
		StringMessage message = new StringMessage("test");
		
		final MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				SecurityContext associatedContext = SecurityContextHolder.getContext();
				assertEquals("Wrong security context", securityContext, associatedContext);
				return null;
			}
		};
		SecurityContextAssociatingHandlerInterceptor associatingInterceptor =
			new SecurityContextAssociatingHandlerInterceptor(handler);
		associatingInterceptor.handle(message);
		assertEquals("Security context no logner set", securityContext, SecurityContextHolder.getContext());
	}


	@SuppressWarnings("serial")
	private static class StubSecurityContext implements SecurityContext {

		StubAuthentication stubAuthentication = new StubAuthentication();

		public Authentication getAuthentication() {
			return stubAuthentication;
		}

		public void setAuthentication(Authentication authentication) {
		}
	}


	@SuppressWarnings("serial")
	private static class StubAuthentication implements Authentication {

		public GrantedAuthority[] getAuthorities() {
			return null;
		}

		public Object getCredentials() {
			return null;
		}

		public Object getDetails() {
			return null;
		}

		public Object getPrincipal() {
			return null;
		}

		public boolean isAuthenticated() {
			return false;
		}

		public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		}

		public String getName() {
			return null;
		}
	}

}
