/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.security.MockAuthenticationManager;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ChannelSecurityInterceptorTests {

	@After
	public void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthenticationException.class)
	public void securedSendWithoutAuthentication() throws Exception {
		MessageChannel channel = getSecuredChannel("ROLE_ADMIN");
		channel.send(new GenericMessage<String>("test"));
	}

	@Test(expected = AccessDeniedException.class)
	public void securedSendWithoutRole() throws Exception {
		MessageChannel channel = getSecuredChannel("ROLE_ADMIN");
		SecurityContext context = SecurityTestUtils.createContext("test", "pwd", "ROLE_USER");
		SecurityContextHolder.setContext(context);
		channel.send(new GenericMessage<String>("test"));
	}

	@Test
	public void securedSendWithRole() throws Exception {
		MessageChannel channel = getSecuredChannel("ROLE_ADMIN");
		SecurityContext context = SecurityTestUtils.createContext("test", "pwd", "ROLE_ADMIN");
		SecurityContextHolder.setContext(context);
		channel.send(new GenericMessage<String>("test"));
	}


	private static MessageChannel getSecuredChannel(String role) throws Exception {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("securedChannel");
		ProxyFactory proxyFactory = new ProxyFactory(channel);
		proxyFactory.addAdvice(createInterceptor(role));
		return (MessageChannel) proxyFactory.getProxy();
	}

	private static ChannelSecurityInterceptor createInterceptor(String role) throws Exception {
		ChannelSecurityMetadataSource securityMetadataSource = new ChannelSecurityMetadataSource();
		securityMetadataSource.addPatternMapping(Pattern.compile("secured.*"), new DefaultChannelAccessPolicy(role, null));
		ChannelSecurityInterceptor interceptor = new ChannelSecurityInterceptor(securityMetadataSource);
		@SuppressWarnings("rawtypes")
		AffirmativeBased accessDecisionManager = new AffirmativeBased(Collections.<AccessDecisionVoter>singletonList(new RoleVoter()));
		accessDecisionManager.afterPropertiesSet();
		interceptor.setAccessDecisionManager(accessDecisionManager);
		interceptor.setAuthenticationManager(new MockAuthenticationManager(true));
		interceptor.afterPropertiesSet();
		return interceptor;
	}

}
