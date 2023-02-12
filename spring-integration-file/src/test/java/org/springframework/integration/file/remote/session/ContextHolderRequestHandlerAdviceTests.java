/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
	Config config;

	@Test
	public void testFlow() {
		final Map<String, Object> headers = Map.of("test-key", "foo");
		config.in().send(new GenericMessage<>("any-payload", headers));

		Message<?> received = config.out().receive(0);
		assertThat(received).isNotNull();
		assertThat(TestUtils.getPropertyValue(config.dsf(), "threadKey", ThreadLocal.class).get()).isNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		MessageChannel in() {
			return MessageChannels.direct("in").get();
		}

		@Bean
		PollableChannel out() {
			return MessageChannels.queue("out").get();
		}

		@Bean
		TestService testService() {
			return new TestService();
		}

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
			final Function<Message<?>, Object> keyProviderFn = m -> m.getHeaders().get("test-key");
			final Consumer<Object> contextSetHookFn = dsf::setThreadKey;
			final Consumer<Object> contextClearHookFn = key -> dsf.clearThreadKey();
			return new ContextHolderRequestHandlerAdvice(keyProviderFn, contextSetHookFn, contextClearHookFn);
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

	@Component
	public static class TestService {

		@ServiceActivator(inputChannel = "in", outputChannel = "out", adviceChain = "advice")
		public String test(Message<String> message) {
			return message.getPayload();
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
