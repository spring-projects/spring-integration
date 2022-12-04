/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.integration.file.remote.session;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ContextHolderRequestHandlerAdviceTests {

	private ContextHolderRequestHandlerAdvice advice;

	@BeforeEach
	public void setUp() {
		advice = new ContextHolderRequestHandlerAdvice();
	}

	@Test
	public void shouldSetKeyProvider() {
		// given
		advice.setKeyProvider(message -> message.getHeaders().get("test-key"));
		final var message = MessageBuilder.withPayload("test-payload").setHeader("test-key", new TestSessionFactory()).build();

		// when
		final var sessionFactory = advice.getKeyProvider().apply(message);

		// then
		assertThat(sessionFactory).isNotNull();
		assertThat(sessionFactory).isInstanceOf(TestSessionFactory.class);
	}

	@Test
	void shouldSetContextSetHook() {
		// given
		final Map<Object, SessionFactory<String>> factories = new HashMap<>();
		final var testSessionFactory = new TestSessionFactory();
		factories.put("test-key", testSessionFactory);
		final var defaultFactory = new TestSessionFactory();
		factories.put("default", defaultFactory);

		final DelegatingSessionFactory<String> delegatingSessionFactory = new DelegatingSessionFactory<>(factories, testSessionFactory);
		advice.setContextSetHook(delegatingSessionFactory::setThreadKey);

		// when
		advice.getContextSetHook().accept("test-key");

		// then
		assertThat(delegatingSessionFactory.getSession()).isNotEqualTo(defaultFactory.mockSession).isEqualTo(testSessionFactory.mockSession);
	}

	@Test
	void shouldSetContextClearHook() {
		// given
		final Map<Object, SessionFactory<String>> factories = new HashMap<>();
		final var testSessionFactory = new TestSessionFactory();
		factories.put("test-key", testSessionFactory);
		final var defaultFactory = new TestSessionFactory();
		factories.put("default", defaultFactory);

		final DelegatingSessionFactory<String> delegatingSessionFactory = new DelegatingSessionFactory<>(factories, defaultFactory);
		advice.setContextClearHook(key -> delegatingSessionFactory.clearThreadKey());

		// when
		advice.getContextClearHook().accept("test-key");

		// then
		assertThat(delegatingSessionFactory.getSession()).isNotEqualTo(testSessionFactory.mockSession).isEqualTo(defaultFactory.mockSession);
	}

	private static class TestSessionFactory implements SessionFactory<String> {

		@SuppressWarnings("unchecked")
		private final Session<String> mockSession = mock(Session.class);

		@Override
		public Session<String> getSession() {
			return this.mockSession;
		}

	}

}
