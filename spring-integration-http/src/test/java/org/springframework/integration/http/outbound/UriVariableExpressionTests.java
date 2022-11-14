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

package org.springframework.integration.http.outbound;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Wallace Wadge
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class UriVariableExpressionTests {

	@Test
	public void testFromMessageWithExpressions() {
		final AtomicReference<URI> uriHolder = new AtomicReference<>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://test/{foo}");
		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setUriVariableExpressions(Collections.singletonMap("foo", parser.parseExpression("payload")));
		handler.setRequestFactory(
				(uri, httpMethod) -> {
					uriHolder.set(uri);
					throw new RuntimeException("intentional");
				});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("bar");
		try {
			handler.handleMessage(message);
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
		}
		assertThat(uriHolder.get().toString()).isEqualTo("http://test/bar");
	}

	/**
	 * Test for INT-3054: Do not break if there are extra uri variables defined in the http outbound gateway.
	 */
	@Test
	public void testFromMessageWithSuperfluousExpressionsInt3054() {
		final AtomicReference<URI> uriHolder = new AtomicReference<>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://test/{foo}");
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> multipleExpressions = new HashMap<>();
		multipleExpressions.put("foo", parser.parseExpression("payload"));
		multipleExpressions.put("extra-to-be-ignored", parser.parseExpression("headers.extra"));
		handler.setUriVariableExpressions(multipleExpressions);
		handler.setRequestFactory(
				(uri, httpMethod) -> {
					uriHolder.set(uri);
					throw new RuntimeException("intentional");
				});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		try {
			handler.handleMessage(new GenericMessage<Object>("bar"));
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
		}
		assertThat(uriHolder.get().toString()).isEqualTo("http://test/bar");
	}

	@Test
	public void testInt3055UriVariablesExpression() {
		final AtomicReference<URI> uriHolder = new AtomicReference<>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://test/{foo}");

		handler.setRequestFactory(
				(uri, httpMethod) -> {
					uriHolder.set(uri);
					throw new RuntimeException("intentional");
				});

		AbstractApplicationContext context = TestUtils.createTestApplicationContext();
		IntegrationRegistrar registrar = new IntegrationRegistrar();
		registrar.registerBeanDefinitions(null, (BeanDefinitionRegistry) context.getBeanFactory());
		context.refresh();
		handler.setBeanFactory(context);
		handler.setUriVariablesExpression(new SpelExpressionParser().parseExpression("headers.uriVariables"));
		handler.afterPropertiesSet();

		Map<String, Object> expressions = new HashMap<>();
		expressions.put("foo", "bar");

		Map<String, ?> expressionsMap = ExpressionEvalMap.from(expressions).usingSimpleCallback().build();

		try {
			handler.handleMessage(MessageBuilder.withPayload("test")
					.setHeader("uriVariables", expressionsMap)
					.build());
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
		}

		assertThat(uriHolder.get().toString()).isEqualTo("http://test/bar");

		expressions.put("foo", new SpelExpressionParser().parseExpression("'bar'.toUpperCase()"));
		try {
			handler.handleMessage(MessageBuilder.withPayload("test")
					.setHeader("uriVariables", expressions)
					.build());
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
		}

		assertThat(uriHolder.get().toString()).isEqualTo("http://test/BAR");

		expressions.put("foo", new SpelExpressionParser().parseExpression("T(Integer).valueOf('42')"));
		try {
			handler.handleMessage(MessageBuilder.withPayload("test")
					.setHeader("uriVariables", expressions)
					.build());
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).contains("Type cannot be found");
		}

		handler.setTrustedSpel(true);
		try {
			handler.handleMessage(MessageBuilder.withPayload("test")
					.setHeader("uriVariables", expressions)
					.build());
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional");
		}
		assertThat(uriHolder.get().toString()).isEqualTo("http://test/42");

		context.close();
	}

}
