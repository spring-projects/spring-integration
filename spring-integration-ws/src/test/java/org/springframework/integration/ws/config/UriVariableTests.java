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
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpUrlConnection;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class UriVariableTests {

	@Autowired
	private ApplicationContext context;

	@Test
	@SuppressWarnings("unchecked")
	public void checkUriVariables() {
		Object gateway = context.getBean("gateway.handler");
		WebServiceTemplate webServiceTemplate = TestUtils.getPropertyValue(gateway, "webServiceTemplate", WebServiceTemplate.class);
		webServiceTemplate = Mockito.spy(webServiceTemplate);
		final AtomicReference<String> uri = new AtomicReference<String>();
		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				uri.set((String) invocation.getArguments()[0]);
				throw new WebServiceIOException("intentional");
			}
		}).when(webServiceTemplate)
				.sendAndReceive(Mockito.anyString(),
						Mockito.any(WebServiceMessageCallback.class),
						(WebServiceMessageExtractor<Object>) Mockito.any(WebServiceMessageExtractor.class));

		new DirectFieldAccessor(gateway).setPropertyValue("webServiceTemplate", webServiceTemplate);

		MessageChannel input = context.getBean("input", MessageChannel.class);
		TestClientInterceptor interceptor = context.getBean("interceptor", TestClientInterceptor.class);
		Message<?> message = MessageBuilder.withPayload("<spring/>").setHeader("x", "integration").build();
		try {
			input.send(message);
		}
		catch (MessageHandlingException e) {
			// expected
			assertThat(e.getCause(), Matchers.is(Matchers.instanceOf(WebServiceIOException.class))); // offline
		}
		assertEquals("http://localhost/spring-integration", uri.get());
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
