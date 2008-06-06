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

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.InsufficientAuthenticationException;

/**
 * @author Jonas Partner
 */
public class SecurityEnforcingChannelInterceptorTests {

	private QueueChannel channel;

	private SecurityEnforcingChannelInterceptor securityChannelInterceptor;


	@Before
	public void setUp() {
		channel = new QueueChannel();
	}


	@Test(expected = AccessDeniedException.class)
	public void testSendSecuredAndAccessDenied() {
		try {
			Runnable decision = new Runnable() {
				public void run() {
					throw new AccessDeniedException("nope");
				}
			};
			this.registerInterceptor(new ConfigurableAccessDecisionManager(decision));
			this.securityChannelInterceptor.setSendSecurityAttributes(new ConfigAttributeDefinition("ROLE_ADMIN"));
			this.channel.send(new StringMessage("test"));
		}
		finally {
			assertEquals("Wrong message count after refused send.", 0, channel.clear().size());
		}
	}

	@Test
	public void testUnsecuredSend() {
		this.registerInterceptor(new ConfigurableAccessDecisionManager(null));
		this.channel.send(new StringMessage("test"));
		assertEquals("Wrong message count after send.", 1,channel.clear().size());
	}

	@Test
	public void testSendSecuredAndAllowed() {
		Runnable decision = new Runnable() {
			public void run() {
			}
		};
		this.registerInterceptor(new ConfigurableAccessDecisionManager(decision));
		this.securityChannelInterceptor.setSendSecurityAttributes(new ConfigAttributeDefinition("ROLE_ADMIN"));
		this.channel.send(new StringMessage("test"));
		assertEquals("Wrong message count after send", 1, channel.clear().size());
	}

	@Test
	public void testReceiveSecuredAndAllowed() {
		Runnable decision = new Runnable() {
			public void run() {
			}
		};
		this.registerInterceptor(new ConfigurableAccessDecisionManager(decision));
		this.securityChannelInterceptor.setReceiveSecurityAttributes(new ConfigAttributeDefinition("ROLE_ADMIN"));
		this.channel.receive(0);
	}

	@Test(expected = AccessDeniedException.class)
	public void testReceiveSecuredAndAccessDenied() {
		Runnable decision = new Runnable() {
			public void run() {
				throw new AccessDeniedException("nope");
			}
		};
		this.registerInterceptor(new ConfigurableAccessDecisionManager(decision));
		this.securityChannelInterceptor.setReceiveSecurityAttributes(new ConfigAttributeDefinition("ROLE_ADMIN"));
		this.channel.receive(0);
	}

	@Test
	public void testReceiveUnsecured() {
		Runnable decision = new Runnable() {
			public void run() {
				throw new AccessDeniedException("nope");
			}
		};
		this.registerInterceptor(new ConfigurableAccessDecisionManager(decision));
		this.channel.receive(0);
	}


	private void registerInterceptor(AccessDecisionManager accessDecisionManager) {
		securityChannelInterceptor = new SecurityEnforcingChannelInterceptor(
				accessDecisionManager, channel);
	
	}


	private static class ConfigurableAccessDecisionManager implements AccessDecisionManager {

		private Runnable decisionRunner;

		public ConfigurableAccessDecisionManager(Runnable decision) {
			this.decisionRunner = decision;
		}

		public void decide(Authentication authentication, Object object, ConfigAttributeDefinition config)
				throws AccessDeniedException, InsufficientAuthenticationException {
			this.decisionRunner.run();
		}

		public boolean supports(ConfigAttribute attribute) {
			return true;
		}

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}
	}

}
