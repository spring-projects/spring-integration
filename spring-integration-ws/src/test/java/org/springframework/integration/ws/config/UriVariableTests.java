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

package org.springframework.integration.ws.config;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpUrlConnection;

/**
 * @author Mark Fisher
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class UriVariableTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void checkUriVariables() {
		MessageChannel input = context.getBean("input", MessageChannel.class);
		TestClientInterceptor interceptor = context.getBean("interceptor", TestClientInterceptor.class);
		Message<?> message = MessageBuilder.withPayload("<FOO/>").setHeader("bar", "BAR").build();
		try {
			input.send(message);
		}
		catch (MessageHandlingException e) {
			// expected
			assertEquals(WebServiceIOException.class, e.getCause().getClass());
		}
		assertEquals("http://localhost/test/FOO/BAR", interceptor.getLastUri().toString());
	}


	private static class TestClientInterceptor implements ClientInterceptor {

		private URI lastUri;

		private URI getLastUri() {
			return this.lastUri;
		}

		public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
			TransportContext tc = TransportContextHolder.getTransportContext();
			if (tc != null && tc.getConnection() instanceof HttpUrlConnection) {
				try {
					this.lastUri = ((HttpUrlConnection) tc.getConnection()).getUri();
				}
				catch (URISyntaxException e) {
					throw new IllegalStateException(e);
				}
			}
			else {
				throw new IllegalStateException("expected HttpUrlConnection as TransportContext");
			}
			return false;
		}

		public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
			return false;
		}

		public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
			return false;
		}
	}

}
