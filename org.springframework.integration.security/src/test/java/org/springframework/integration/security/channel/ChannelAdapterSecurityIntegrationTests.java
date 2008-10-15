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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.AuthenticationException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
public class ChannelAdapterSecurityIntegrationTests extends AbstractJUnit4SpringContextTests {

	@Autowired
	@Qualifier("securedChannelAdapter")
	MessageChannel securedChannelAdapter;

	@Autowired
	@Qualifier("unsecuredChannelAdapter")
	MessageChannel unsecuredChannelAdapter;

	@Autowired
	TestConsumer testConsumer;


	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}


	@Test
	@DirtiesContext
	public void testSecuredWithPermission() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		securedChannelAdapter.send(new StringMessage("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test(expected = AccessDeniedException.class)
	@DirtiesContext
	public void testSecuredWithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		securedChannelAdapter.send(new StringMessage("test"));
	}

	@Test(expected = AuthenticationException.class)
	@DirtiesContext
	public void testSecuredWithoutAuthenticating() {
		securedChannelAdapter.send(new StringMessage("test"));
	}

	@Test
	@DirtiesContext
	public void testUnsecuredAsAdmin() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		unsecuredChannelAdapter.send(new StringMessage("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	@DirtiesContext
	public void testUnsecuredAsUser() {
		login("bob", "bobspassword", "ROLE_USER");
		unsecuredChannelAdapter.send(new StringMessage("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	@DirtiesContext
	public void testUnsecuredWithoutAuthenticating() {
		unsecuredChannelAdapter.send(new StringMessage("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}


	private void login(String username, String password, String... roles) {
		SecurityContext context = SecurityTestUtils.createContext(username, password, roles);
		SecurityContextHolder.setContext(context);
	}

}
