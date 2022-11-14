/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.security.channel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.integration.security.TestHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChannelAdapterSecurityIntegrationTests {

	@Autowired
	@Qualifier("securedChannelAdapter")
	MessageChannel securedChannelAdapter;

	@Autowired
	@Qualifier("securedChannelAdapter")
	MessageChannel securedChannelAdapter2;

	@Autowired
	@Qualifier("unsecuredChannelAdapter")
	MessageChannel unsecuredChannelAdapter;

	@Autowired
	@Qualifier("queueChannel")
	MessageChannel queueChannel;

	@Autowired
	@Qualifier("securedChannelQueue")
	PollableChannel securedChannelQueue;

	@Autowired
	@Qualifier("errorChannel")
	PollableChannel errorChannel;

	@Autowired
	TestHandler testConsumer;


	@AfterEach
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}


	@Test
	public void testSecuredWithNotEnoughPermission() {
		login("bob", "bobspassword", "ROLE_ADMINA");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannelAdapter.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void testSecuredWithPermission() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		securedChannelAdapter.send(new GenericMessage<>("test"));
		securedChannelAdapter2.send(new GenericMessage<>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(2);
	}

	@Test
	public void testSecurityContextPropagation() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		this.queueChannel.send(new GenericMessage<>("test"));
		Message<?> receive = this.securedChannelQueue.receive(10000);
		assertThat(receive).isNotNull();

		SecurityContextHolder.clearContext();

		this.queueChannel.send(new GenericMessage<>("test"));
		Message<?> errorMessage = this.errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		Object payload = errorMessage.getPayload();
		assertThat(payload).isInstanceOf(MessageDeliveryException.class);
		assertThat(((MessageDeliveryException) payload).getCause())
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void testSecuredWithoutPermission() {
		login("bob", "bobspassword", "ROLE_USER");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannelAdapter.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void testSecured2WithoutPermission() {
		login("bob", "bobspassword", "ROLE_USER");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannelAdapter2.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void testSecuredWithoutAuthenticating() {
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannelAdapter.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void testUnsecuredAsAdmin() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		unsecuredChannelAdapter.send(new GenericMessage<String>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(1);
	}

	@Test
	public void testUnsecuredAsUser() {
		login("bob", "bobspassword", "ROLE_USER");
		unsecuredChannelAdapter.send(new GenericMessage<String>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(1);
	}

	@Test
	public void testUnsecuredWithoutAuthenticating() {
		unsecuredChannelAdapter.send(new GenericMessage<String>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(1);
	}


	private void login(String username, String password, String... roles) {
		SecurityContext context = SecurityTestUtils.createContext(username, password, roles);
		SecurityContextHolder.setContext(context);
	}

}
