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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.MessageHeader;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * @author Jonas Partner
 */
public class SecurityContextPropagatingChannelInterceptorTests {

	private QueueChannel channel;

	private SecurityContextPropagatingChannelInterceptor securityPropogatingChannelInterceptor;

	private StubSecurityContext securityContext;

	@Before
	public void setUp() {
		this.channel = new QueueChannel();
		this.securityPropogatingChannelInterceptor = new SecurityContextPropagatingChannelInterceptor();
		this.channel.addInterceptor(securityPropogatingChannelInterceptor);
		this.securityContext = new StubSecurityContext();
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void testPropogationWhenSecurityContextExists() {
		this.associateContextWithThread();
		StringMessage message = new StringMessage("test");
		this.channel.send(message);
		message = (StringMessage) channel.receive(0);
		MessageHeader header = message.getHeader();
		assertTrue("No security context attribute found in header.", header.getAttributeNames().contains(
				SecurityContextUtils.SECURITY_CONTEXT_HEADER_ATTRIBUTE));
		SecurityContext contextFromHeader = SecurityContextUtils.getSecurityContextFromHeader(message);
		assertEquals("Incorrect security context in message header.", securityContext, contextFromHeader);
	}

	@Test
	public void testHeaderNotSetWhenNoSecurityContextExists() {
		StringMessage message = new StringMessage("test");
		channel.send(message);
		message = (StringMessage) channel.receive(0);
		MessageHeader header = message.getHeader();
		assertFalse("Security context header found when no security context existed.", header.getAttributeNames()
				.contains(SecurityContextUtils.SECURITY_CONTEXT_HEADER_ATTRIBUTE));
	}

	private void associateContextWithThread() {
		SecurityContextHolder.setContext(securityContext);
	}

	@SuppressWarnings("serial")
	private static class StubSecurityContext implements SecurityContext {

		private Authentication authentication = new Authentication() {

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
		};

		public Authentication getAuthentication() {
			return authentication;
		}

		public void setAuthentication(Authentication authentication) {
		}
	}

}
