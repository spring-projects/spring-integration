/*
 * Copyright 2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.integration.security.TestHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChannelSecurityInterceptorFactoryBeanTests {

	@Autowired
	MessageChannel securedChannel;

	@Autowired
	MessageChannel securedChannel2;

	@Autowired
	MessageChannel unsecuredChannel;

	@Autowired
	TestHandler testConsumer;


	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}


	@Test(expected = AccessDeniedException.class)
	public void testSecuredWithNotEnoughPermission() {
		login("bob", "bobspassword", "ROLE_ADMINA");
		securedChannel.send(new GenericMessage<String>("test"));
	}

	@Test
	public void testSecuredWithPermission() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		securedChannel.send(new GenericMessage<String>("test"));
		securedChannel2.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 2, testConsumer.sentMessages.size());
	}

	@Test(expected = AccessDeniedException.class)
	public void testSecuredWithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		securedChannel.send(new GenericMessage<String>("test"));
	}

	@Test(expected = AccessDeniedException.class)
	public void testSecured2WithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		securedChannel2.send(new GenericMessage<String>("test"));
	}

	@Test(expected = AuthenticationException.class)
	public void testSecuredWithoutAuthenticating() {
		securedChannel.send(new GenericMessage<String>("test"));
	}

	@Test
	public void testUnsecuredAsAdmin() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		unsecuredChannel.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	public void testUnsecuredAsUser() {
		login("bob", "bobspassword", "ROLE_USER");
		unsecuredChannel.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	public void testUnsecuredWithoutAuthenticating() {
		unsecuredChannel.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}


	private void login(String username, String password, String... roles) {
		SecurityContext context = SecurityTestUtils.createContext(username, password, roles);
		SecurityContextHolder.setContext(context);
	}


	@Configuration
	@EnableIntegration
	@ImportResource("classpath:org/springframework/integration/security/config/commonSecurityConfiguration.xml")
	public static class ContextConfiguration {

		@Bean
		public SubscribableChannel securedChannel() {
			return new DirectChannel();
		}

		@Bean
		public SubscribableChannel securedChannel2() {
			return new DirectChannel();
		}

		@Bean
		public SubscribableChannel unsecuredChannel() {
			return new DirectChannel();
		}

		@Bean
		public TestHandler testHandler() {
			TestHandler testHandler = new TestHandler();
			this.securedChannel().subscribe(testHandler);
			this.securedChannel2().subscribe(testHandler);
			this.unsecuredChannel().subscribe(testHandler);
			return testHandler;
		}

		@Bean
		public ChannelSecurityInterceptorFactoryBean channelSecurityInterceptor() {
			return new ChannelSecurityInterceptorFactoryBean()
					.accessPolicy("securedChannel.*", "ROLE_ADMIN, ROLE_PRESIDENT");
		}

	}

}
