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

package org.springframework.integration.security.config;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class SecuredParserTests extends AbstractJUnit4SpringContextTests{

	@Autowired
	private AuthenticationProvider provider;
	
	@Autowired
	@Qualifier("unsecured")
	public MessageChannel unsecuredChannel;
	
	@Autowired
	@Qualifier("adminRequiredForSend")
	public MessageChannel adminRequiredForSend;
	
	@Autowired
	@Qualifier("adminRequiredForReceive")
	public MessageChannel adminRequiredForReceive;
	
	@Autowired
	@Qualifier("adminRequiredForSendAndReceive")
	public MessageChannel adminRequiredForSendAndReceive;


	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}


	@Test
	public void testNoSecurityRestrictionsOnChannel() {
		unsecuredChannel.send(new StringMessage("testUnsecured"));
		assertNotNull("Message not received ", unsecuredChannel.receive(0));
	}

	@Test
	public void testAdminRequiredForSendWithAccessGranted() {
		login("jimi", "jimispassword");
		adminRequiredForSend.send(new StringMessage("testmessage"));
		SecurityContextHolder.clearContext();
		assertNotNull("Message not received", adminRequiredForSend.receive(0));
	}

	@Test(expected=AccessDeniedException.class)
	public void testAdminRequiredForSendWithAccessDenied() {
		login("bob", "bobspassword");
		adminRequiredForSend.send(new StringMessage("testmessage"));
	}

	@Test
	public void testAdminRequiredForReceiveWithAccessGranted(){
		adminRequiredForReceive.send(new StringMessage("testmessage"));
		login("jimi", "jimispassword");
		assertNotNull("Message not received", adminRequiredForReceive.receive(0));
	}

	@Test(expected=AccessDeniedException.class)
	public void testAdminRequiredForReceiveWithAccessDenied() {
		adminRequiredForReceive.send(new StringMessage("testmessage"));
		login("bob", "bobspassword");
		adminRequiredForReceive.receive(0);
	}

	@Test
	public void testAdminRequiredForSendAndReceiveWithSendAccessGranted() {
		login("jimi", "jimispassword");
		adminRequiredForSendAndReceive.send(new StringMessage("test"));
	}

	@Test(expected=AccessDeniedException.class)
	public void testAdminRequiredForSendAndReceiveWithSendAccessDenied() {
		login("bob", "bobspassword");
		adminRequiredForSendAndReceive.send(new StringMessage("test"));
	}

	@Test
	public void testAdminRequiredForSendAndReceiveWithReceiveAccessGranted() {
		login("jimi", "jimispassword");
		adminRequiredForSendAndReceive.send(new StringMessage("test"));
		assertNotNull("Message not received", adminRequiredForSendAndReceive.receive(0));
	}

	@Test(expected=AccessDeniedException.class)
	public void testAdminRequiredForSendAndReceiveWithReceiveAccessDenied() {
		login("bob","bobspassword");
		adminRequiredForSendAndReceive.receive(0);
	}


	private void login(String username, String password) {
		UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken)
				provider.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		SecurityContext context = new SecurityContextImpl();
		context.setAuthentication(authToken);
		SecurityContextHolder.setContext(context);
	}

}
