/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.http.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Wallace Wadge
 * @author Gary Russell
 * @since 2.0
 */
public class UriVariableExpressionTests {

	@Test
	public void testFromMessageWithExpressions() throws Exception {
		final AtomicReference<URI> uriHolder = new AtomicReference<URI>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://test/{foo}");
		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setUriVariableExpressions(Collections.singletonMap("foo", parser.parseExpression("payload")));
		handler.setRequestFactory(new SimpleClientHttpRequestFactory() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				uriHolder.set(uri);
				throw new RuntimeException("intentional");
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<?> message = new GenericMessage<Object>("bar");
		try {
			handler.handleMessage(message);
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertEquals("intentional", e.getCause().getMessage());
		}
		assertEquals("http://test/bar", uriHolder.get().toString());
	}

	/** Test for INT-3054: Do not break if there are extra uri variables defined in the http outbound gateway. */
	@Test
	public void testFromMessageWithSuperfluousExpressionsInt3054() throws Exception {
		final AtomicReference<URI> uriHolder = new AtomicReference<URI>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://test/{foo}");
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> multipleExpressions = new HashMap<String, Expression>();
		multipleExpressions.put("foo", parser.parseExpression("payload"));
		multipleExpressions.put("extra-to-be-ignored", parser.parseExpression("headers.extra"));
		handler.setUriVariableExpressions(multipleExpressions);
		handler.setRequestFactory(new SimpleClientHttpRequestFactory() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				uriHolder.set(uri);
				throw new RuntimeException("intentional");
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		try {
			handler.handleMessage(new GenericMessage<Object>("bar"));
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertEquals("intentional", e.getCause().getMessage());
		}
		assertEquals("http://test/bar", uriHolder.get().toString());
	}

	@Test
	public void testInt3055UriVariablesExpression() throws Exception {
		final AtomicReference<URI> uriHolder = new AtomicReference<URI>();
		HttpRequestExecutingMessageHandler handler = new HttpRequestExecutingMessageHandler("http://test/{foo}");

		handler.setRequestFactory(new SimpleClientHttpRequestFactory() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				uriHolder.set(uri);
				throw new RuntimeException("intentional");
			}
		});
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setUriVariablesExpression(new SpelExpressionParser().parseExpression("headers.uriVariables"));
		handler.afterPropertiesSet();

		Map<String, String> expressions = new HashMap<String, String>();
		expressions.put("foo", "bar");

		Map<String, ?> expressionsMap = ExpressionEvalMap.from(expressions).usingSimpleCallback().build();

		try {
			handler.handleMessage(MessageBuilder.withPayload("test").setHeader("uriVariables", expressionsMap).build());
			fail("Exception expected.");
		}
		catch (Exception e) {
			assertEquals("intentional", e.getCause().getMessage());
		}

		assertEquals("http://test/bar", uriHolder.get().toString());
	}

}
