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

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.Target;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.InsufficientAuthenticationException;

/**
 * 
 * @author Jonas Partner
 *
 */
public class TargetSecuringAdvisorTests {

	public Object proxy(Object target, TargetSecuringAdvisor advisor) {
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.addAdvisor(advisor);

		return proxyFactory.getProxy();

	}

	@Test(expected = AccessDeniedException.class)
	public void testTargetSendAdvised() {
		TargetSecuringAdvisor advisor = new TargetSecuringAdvisor(new AlwaysDenyAccessDecisionManager(), "ROLE_ADMIN");
		Target target = (Target) proxy(new TestTarget(), advisor);
		target.send(new StringMessage("test"));
	}

	@Test(expected = AccessDeniedException.class)
	public void testBlockingTargetSendAdvised() {
		TargetSecuringAdvisor advisor = new TargetSecuringAdvisor(new AlwaysDenyAccessDecisionManager(), "ROLE_ADMIN");
		Target target = (Target) proxy(new BlockingTestTarget(), advisor);
		target.send(new StringMessage("test"));
	}

	@Test(expected = AccessDeniedException.class)
	public void testBlockingTargetSendWithTimeoutAdvised() {
		TargetSecuringAdvisor advisor = new TargetSecuringAdvisor(new AlwaysDenyAccessDecisionManager(), "ROLE_ADMIN");
		BlockingTarget target = (BlockingTarget) proxy(new BlockingTestTarget(), advisor);
		target.send(new StringMessage("test"), 10l);
	}

	@Test
	public void testTargetSendNotFromTargetInterface() {
		TargetSecuringAdvisor advisor = new TargetSecuringAdvisor(new AlwaysDenyAccessDecisionManager(), "ROLE_ADMIN");
		OtherSend target = (OtherSend) proxy(new TestTarget(), advisor);
		target.send(10l);
	}

	static interface OtherSend {

		public void send(long l);
	}

	static class AlwaysDenyAccessDecisionManager implements AccessDecisionManager {

		public void decide(Authentication authentication, Object object, ConfigAttributeDefinition config)
				throws AccessDeniedException, InsufficientAuthenticationException {
			throw new AccessDeniedException("dave");
		}

		public boolean supports(ConfigAttribute attribute) {
			return true;
		}

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}

	}

	static class TestTarget implements Target, OtherSend {

		boolean invoked;

		public boolean send(Message<?> message) {
			invoked = true;
			return false;
		}

		public void send(long a) {

		}

	}

	static class BlockingTestTarget implements BlockingTarget {

		boolean invoked;

		public boolean send(Message<?> message) {
			invoked = true;
			return false;
		}

		public void send() {
		}

		public boolean send(Message<?> message, long timeout) {
			return false;
		}

	}

}
