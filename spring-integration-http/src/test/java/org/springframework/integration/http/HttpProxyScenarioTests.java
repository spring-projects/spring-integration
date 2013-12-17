/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HttpProxyScenarioTests {

	private final HandlerAdapter handlerAdapter = new HttpRequestHandlerAdapter();

	@Autowired
	private HandlerMapping handlerMapping;

	@Autowired
	@Qualifier("proxyGateway.handler")
	private HttpRequestExecutingMessageHandler handler;

	@Autowired
	private PollableChannel checkHeadersChannel;

	@Test
	public void testHttpProxyScenario() throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		Calendar c = Calendar.getInstance();
		c.set(Calendar.MILLISECOND, 0);

		final long ifModifiedSince = c.getTimeInMillis();
		String ifModifiedSinceValue = dateFormat.format(ifModifiedSince);

		c.add(Calendar.DATE, -1);
		long ifUnmodifiedSince = c.getTimeInMillis();
		final String ifUnmodifiedSinceValue = dateFormat.format(ifUnmodifiedSince);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.setQueryString("foo=bar&FOO=BAR");

		request.addHeader("If-Modified-Since", ifModifiedSinceValue);
		request.addHeader("If-Unmodified-Since", ifUnmodifiedSinceValue);
		request.addHeader("Connection", "Keep-Alive");

		Object handler = this.handlerMapping.getHandler(request).getHandler();
		assertNotNull(handler);

		MockHttpServletResponse response = new MockHttpServletResponse();

		RestTemplate template = Mockito.spy(new RestTemplate());

		Mockito.doAnswer(new Answer<ResponseEntity<?>>() {
			@Override
			public ResponseEntity<?> answer(InvocationOnMock invocation) throws Throwable {
				URI uri = (URI) invocation.getArguments()[0];
				assertEquals(new URI("http://testServer/test?foo=bar&FOO=BAR"), uri);
				HttpEntity<?> httpEntity = (HttpEntity<?>) invocation.getArguments()[2];
				HttpHeaders httpHeaders = httpEntity.getHeaders();
				assertEquals(ifModifiedSince, httpHeaders.getIfModifiedSince());
				assertEquals(ifUnmodifiedSinceValue, httpHeaders.getFirst("If-Unmodified-Since"));
				assertEquals("Keep-Alive", httpHeaders.getFirst("Connection"));

				MultiValueMap<String, String> responseHeaders = new LinkedMultiValueMap<String, String>(httpHeaders);
				responseHeaders.set("Connection", "close");
				return new ResponseEntity<Object>(responseHeaders, HttpStatus.OK);
			}
		}).when(template).exchange(Mockito.any(URI.class), Mockito.any(HttpMethod.class),
				Mockito.any(HttpEntity.class),  (Class<?>) Mockito.any(Class.class));

		PropertyAccessor dfa = new DirectFieldAccessor(this.handler);
		dfa.setPropertyValue("restTemplate", template);

		RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);

		this.handlerAdapter.handle(request, response, handler);

		assertNull(response.getHeaderValue("If-Modified-Since"));
		assertNull(response.getHeaderValue("If-Unmodified-Since"));
		assertEquals("close", response.getHeaderValue("Connection"));

		Message<?> message = this.checkHeadersChannel.receive(2000);
		MessageHeaders headers = message.getHeaders();

		assertEquals(ifModifiedSince, headers.get("If-Modified-Since"));
		assertEquals(ifUnmodifiedSince, headers.get("If-Unmodified-Since"));

		RequestContextHolder.resetRequestAttributes();
	}

}
