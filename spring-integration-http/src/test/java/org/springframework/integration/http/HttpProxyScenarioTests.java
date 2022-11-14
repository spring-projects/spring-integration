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

package org.springframework.integration.http;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class HttpProxyScenarioTests {

	private final HandlerAdapter handlerAdapter = new HttpRequestHandlerAdapter();

	@Autowired
	private HandlerMapping handlerMapping;

	@Autowired
	@Qualifier("proxyGateway.handler")
	private HttpRequestExecutingMessageHandler handler;

	@Autowired
	@Qualifier("proxyGatewaymp.handler")
	private HttpRequestExecutingMessageHandler handlermp;

	@Autowired
	private PollableChannel checkHeadersChannel;

	@Test
	public void testHttpProxyScenario() throws Exception {
		ZoneId GMT = ZoneId.of("GMT");
		DateTimeFormatter dateTimeFormatter =
				DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(GMT);

		Calendar c = Calendar.getInstance();
		c.set(Calendar.MILLISECOND, 0);

		final long ifModifiedSince = c.getTimeInMillis();
		Instant instant = Instant.ofEpochMilli(ifModifiedSince);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, GMT);
		String ifModifiedSinceValue = dateTimeFormatter.format(zonedDateTime);

		c.add(Calendar.DATE, -1);
		long ifUnmodifiedSince = c.getTimeInMillis();
		instant = Instant.ofEpochMilli(ifUnmodifiedSince);
		zonedDateTime = ZonedDateTime.ofInstant(instant, GMT);
		final String ifUnmodifiedSinceValue = dateTimeFormatter.format(zonedDateTime);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.setQueryString("foo=bar&FOO=BAR");

		request.addHeader("If-Modified-Since", ifModifiedSinceValue);
		request.addHeader("If-Unmodified-Since", ifUnmodifiedSinceValue);
		request.addHeader("Connection", "Keep-Alive");
		request.setContentType("text/plain");

		Object handler = this.handlerMapping.getHandler(request).getHandler();
		assertThat(handler).isNotNull();

		MockHttpServletResponse response = new MockHttpServletResponse();

		RestTemplate template = Mockito.spy(new RestTemplate());

		final String contentDispositionValue = "attachment; filename=\"test.txt\"";

		Mockito.doAnswer(invocation -> {
					String uri = invocation.getArgument(0);
					assertThat(uri).isEqualTo("http://testServer/test?foo=bar&FOO=BAR");
					HttpEntity<?> httpEntity = (HttpEntity<?>) invocation.getArguments()[2];
					HttpHeaders httpHeaders = httpEntity.getHeaders();
					assertThat(httpHeaders.getIfModifiedSince()).isEqualTo(ifModifiedSince);
					assertThat(httpHeaders.getFirst("If-Unmodified-Since")).isEqualTo(ifUnmodifiedSinceValue);
					assertThat(httpHeaders.getFirst("Connection")).isEqualTo("Keep-Alive");

					MultiValueMap<String, String> responseHeaders = new LinkedMultiValueMap<>(httpHeaders);
					responseHeaders.set("Connection", "close");
					responseHeaders.set("Content-Disposition", contentDispositionValue);
					return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
				}).when(template)
				.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class),
						Mockito.any(HttpEntity.class), (Class<?>) isNull(), Mockito.anyMap());

		PropertyAccessor dfa = new DirectFieldAccessor(this.handler);
		dfa.setPropertyValue("restTemplate", template);

		RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);

		this.handlerAdapter.handle(request, response, handler);

		assertThat(response.getHeaderValue("If-Modified-Since")).isEqualTo(ifModifiedSinceValue);
		assertThat(response.getHeaderValue("If-Unmodified-Since")).isEqualTo(ifUnmodifiedSinceValue);
		assertThat(response.getHeaderValue("Connection")).isEqualTo("close");
		assertThat(response.getHeader("Content-Disposition")).isEqualTo(contentDispositionValue);
		assertThat(response.getContentType()).isEqualTo("text/plain");

		Message<?> message = this.checkHeadersChannel.receive(2000);
		MessageHeaders headers = message.getHeaders();

		assertThat(headers.get("If-Modified-Since")).isEqualTo(ifModifiedSince);
		assertThat(headers.get("If-Unmodified-Since")).isEqualTo(ifUnmodifiedSince);

		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testHttpMultipartProxyScenario() throws Exception {
		MockHttpServletRequest request =
				MockMvcRequestBuilders.multipart("/testmp")
						.file("foo", "foo".getBytes())
						.contentType("multipart/form-data;boundary=----WebKitFormBoundarywABD2xqC1FLBijlQ")
						.header("Connection", "Keep-Alive")
						.buildRequest(null);

		Object handler = this.handlerMapping.getHandler(request).getHandler();
		assertThat(handler).isNotNull();

		MockHttpServletResponse response = new MockHttpServletResponse();

		RestTemplate template = Mockito.spy(new RestTemplate());
		Mockito.doAnswer(invocation -> {
					String uri = invocation.getArgument(0);
					assertThat(uri).isEqualTo("http://testServer/testmp");
					HttpEntity<?> httpEntity = (HttpEntity<?>) invocation.getArguments()[2];
					HttpHeaders httpHeaders = httpEntity.getHeaders();
					assertThat(httpHeaders.getFirst("Connection")).isEqualTo("Keep-Alive");
					assertThat(httpHeaders.getContentType().toString())
							.isEqualTo("multipart/form-data;boundary=----WebKitFormBoundarywABD2xqC1FLBijlQ");

					HttpEntity<?> entity = (HttpEntity<?>) invocation.getArguments()[2];
					assertThat(entity.getBody()).isInstanceOf(MultiValueMap.class);
					assertThat(((MultiValueMap<String, ?>) entity.getBody()).getFirst("foo"))
							.isEqualTo("foo".getBytes());

					MultiValueMap<String, String> responseHeaders = new LinkedMultiValueMap<>(httpHeaders);
					responseHeaders.set("Connection", "close");
					responseHeaders.set("Content-Type", "text/plain");
					return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
				}).when(template)
				.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class),
						Mockito.any(HttpEntity.class), (Class<?>) isNull(), Mockito.anyMap());

		PropertyAccessor dfa = new DirectFieldAccessor(this.handlermp);
		dfa.setPropertyValue("restTemplate", template);

		RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);

		this.handlerAdapter.handle(request, response, handler);

		assertThat(response.getHeaderValue("Connection")).isEqualTo("close");
		assertThat(response.getContentType()).isEqualTo("text/plain");

		RequestContextHolder.resetRequestAttributes();
	}

}
