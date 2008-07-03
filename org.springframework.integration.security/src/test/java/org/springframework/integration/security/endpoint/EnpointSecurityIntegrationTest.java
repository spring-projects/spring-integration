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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.security.SecurityTestUtil;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration
public class EnpointSecurityIntegrationTest extends AbstractJUnit4SpringContextTests {

	@Autowired
	MessageChannel channel;

	@Autowired
	TestTarget testTarget;

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DirtiesContext
	public void testWithPermision() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		channel.send(new StringMessage("test"));
		assertEquals("Wrong size of message list in target ", 1, testTarget.sentMessages.size());
	}

	/**
	 * 
	 */
	@Test(expected = AccessDeniedException.class)
	@DirtiesContext
	public void testWithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		channel.send(new StringMessage("test"));
		assertEquals("Wrong size of message list in target ", 1, testTarget.sentMessages.size());
	}

	public void login(String username, String password, String... roles) {
		SecurityContext context = SecurityTestUtil.createContext(username, password, roles);
		SecurityContextHolder.setContext(context);
	}

}
