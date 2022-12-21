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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Adel Haidar
 *
 * @since 6.1
 *
 */
@SpringJUnitConfig
public class ContextHolderRequestHandlerAdviceTests {

	@Autowired
	TestSessionFactory foo;

	@Autowired
	TestSessionFactory bar;

	@Autowired
	ContextHolderRequestHandlerAdvice advice;

	@Autowired
	MessageChannel in;

	@Autowired
	PollableChannel out;

	@Autowired
	DefaultSessionFactoryLocator<String> sessionFactoryLocator;

	@Test
	public void testFlow() throws IOException {
		given(foo.mockSession.list(anyString()))
				.willReturn(new String[0]);
		final Map<String, Object> headers = Map.of("foo", "foo");
		this.in.send(new GenericMessage<>("foo", headers));
		verify(foo.mockSession).list("foo/");
	}

	@Configuration
	@ImportResource(
			"classpath:/org/springframework/integration/file/remote/session/context-holder-request-handler-advice-context.xml")
	@EnableIntegration
	public static class Config {

		@Bean
		TestSessionFactory foo() {
			return new TestSessionFactory();
		}

		@Bean
		TestSessionFactory bar() {
			return new TestSessionFactory();
		}

		@Bean
		ContextHolderRequestHandlerAdvice advice() {
			final DelegatingSessionFactory<String> dsf = dsf();
			final var contextHolderRequestHandlerAdvice = new ContextHolderRequestHandlerAdvice();
			contextHolderRequestHandlerAdvice.setKeyProvider(m -> m.getHeaders().get("foo"));
			contextHolderRequestHandlerAdvice.setContextSetHook(dsf::setThreadKey);
			contextHolderRequestHandlerAdvice.setContextClearHook(c -> dsf.clearThreadKey());
			return contextHolderRequestHandlerAdvice;
		}

		@Bean
		DelegatingSessionFactory<String> dsf() {
			SessionFactoryLocator<String> sff = sessionFactoryLocator();
			return new DelegatingSessionFactory<>(sff);
		}

		@Bean
		public SessionFactoryLocator<String> sessionFactoryLocator() {
			Map<Object, SessionFactory<String>> factories = new HashMap<>();
			factories.put("foo", foo());
			TestSessionFactory bar = bar();
			factories.put("bar", bar);
			return new DefaultSessionFactoryLocator<>(factories, bar);
		}

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
