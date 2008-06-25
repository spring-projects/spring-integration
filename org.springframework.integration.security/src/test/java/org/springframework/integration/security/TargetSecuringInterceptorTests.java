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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.Target;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.integration.security.TargetSecuringInterceptor;
import org.springframework.integration.security.config.SecurityTestUtil;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.vote.AccessDecisionVoter;
import org.springframework.security.vote.AuthenticatedVoter;
import org.springframework.security.vote.RoleVoter;
import org.springframework.security.vote.UnanimousBased;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Jonas Partner
 *
 */
public class TargetSecuringInterceptorTests {
	
	
	UnanimousBased accessDecisionManager;
	
	@Before
	public void setup(){
		accessDecisionManager = new UnanimousBased();
		List<AccessDecisionVoter> voterList = new ArrayList<AccessDecisionVoter>();
		voterList.add(new AuthenticatedVoter());
		voterList.add(new RoleVoter());
		accessDecisionManager.setDecisionVoters(voterList);
	}
	
	
	
	public Object createProxy(Object target,String securityAttributes, AccessDecisionManager accessDecisionManager){
		TargetSecuringInterceptor interceptor = new TargetSecuringInterceptor(new ConfigAttributeDefinition(StringUtils.tokenizeToStringArray(securityAttributes,",")), accessDecisionManager);
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice(interceptor);
		return factory.getProxy();
	}
	
	@Test(expected=AccessDeniedException.class)
	public void testAccessDenied(){
		Target proxiedTarget = (Target) createProxy(new TestTarget(), "IS_AUTHENTICATED_FULLY, ROLE_ADMIN", accessDecisionManager);
		SecurityContext sctx = SecurityTestUtil.createContext("bob", "password", "IS_AUTHENTICATED_ANONYMOUSLY", "ROLE_USER");
		StringMessage message = new StringMessage("test");
		SecurityContextUtils.setSecurityContextHeader(sctx, message);
		proxiedTarget.send(message);
	}
	
	@Test
	public void testAccessGranted(){
		Target proxiedTarget = (Target) createProxy(new TestTarget(), "IS_AUTHENTICATED_FULLY, ROLE_ADMIN", accessDecisionManager);
		SecurityContext sctx = SecurityTestUtil.createContext("bob", "password", "IS_AUTHENTICATED_ANONYMOUSLY", "ROLE_USER", "ROLE_ADMIN");
		StringMessage message = new StringMessage("test");
		SecurityContextUtils.setSecurityContextHeader(sctx, message);
		proxiedTarget.send(message);
	}
	
	
	@Test(expected=RuntimeException.class)
	public void testNotAuthenticated(){
		Target proxiedTarget = (Target) createProxy(new TestTarget(), "IS_AUTHENTICATED_FULLY, ROLE_ADMIN", accessDecisionManager);
		StringMessage message = new StringMessage("test");
		proxiedTarget.send(message);
	}
	

	static class TestTarget implements Target{

		boolean invoked;
		
		public boolean send(Message<?> message) {
			invoked = true;
			return false;
		}

		public void send(long l) {
			// TODO Auto-generated method stub
			
		}
	}

}
