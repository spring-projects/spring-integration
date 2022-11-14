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

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ChannelSecurityInterceptorTests {

	@AfterEach
	public void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void securedSendWithoutAuthentication() {
		MessageChannel channel = getSecuredChannel();
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void securedSendWithoutRole() {
		MessageChannel channel = getSecuredChannel();
		SecurityContext context = SecurityTestUtils.createContext("test", "pwd", "ROLE_USER");
		SecurityContextHolder.setContext(context);
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void securedSendWithRole() {
		MessageChannel channel = getSecuredChannel();
		SecurityContext context = SecurityTestUtils.createContext("test", "pwd", "ROLE_ADMIN");
		SecurityContextHolder.setContext(context);
		channel.send(new GenericMessage<>("test"));
	}


	private static MessageChannel getSecuredChannel() {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("securedChannel");
		channel.addInterceptor(new AuthorizationChannelInterceptor(AuthorityAuthorizationManager.hasRole("ADMIN")));
		return channel;
	}

}
