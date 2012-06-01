/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Dave Syer
 * @author Mark Fisher
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
		Message<?> message = new GenericMessage<Object>("bar");
		Exception exception = null;
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			exception = e;
		}
		assertEquals("intentional", exception.getCause().getMessage());
		assertEquals("http://test/bar", uriHolder.get().toString());
	}

}
