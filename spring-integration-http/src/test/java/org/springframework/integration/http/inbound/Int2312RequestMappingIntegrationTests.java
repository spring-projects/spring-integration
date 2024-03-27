/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class Int2312RequestMappingIntegrationTests extends AbstractHttpInboundTests {

	public static final String TEST_PATH = "/test/{value}";

	public static final String TEST_STRING_MULTIPLE_PATHS = "Multiple Paths The Same Endpoint";

	@Autowired
	private HandlerMapping handlerMapping;

	@Autowired
	private SubscribableChannel toLowerCaseChannel;

	private final HandlerAdapter handlerAdapter = new HttpRequestHandlerAdapter();

	@Test
	public void testMultiplePathsTheSameEndpoint() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/path1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		Object handler = this.handlerMapping.getHandler(request).getHandler();
		this.handlerAdapter.handle(request, response, handler);
		assertThat(response.getContentAsString()).isEqualTo(TEST_STRING_MULTIPLE_PATHS);

		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/path2");
		response = new MockHttpServletResponse();
		handler = this.handlerMapping.getHandler(request).getHandler();
		this.handlerAdapter.handle(request, response, handler);
		assertThat(response.getContentAsString()).isEqualTo(TEST_STRING_MULTIPLE_PATHS);
	}

	@Test
	@SuppressWarnings("unchecked")
	//INT-1362
	public void testURIVariablesAndHeaders() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		String testRequest = "aBc;q1=1;q2=2";
		String requestURI = "/test/" + testRequest;
		request.setRequestURI(requestURI);
		request.setContentType("text/plain");
		final Map<String, String> params = new HashMap<>();
		params.put("foo", "bar");
		request.setParameters(params);
		request.setContent("hello".getBytes());
		final Cookie cookie = new Cookie("foo", "bar");
		request.setCookies(cookie);
		request.addHeader("toLowerCase", true);

		//See org.springframework.web.servlet.FrameworkServlet#initContextHolders
		final RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);

		this.toLowerCaseChannel.subscribe(message -> {
			MessageHeaders headers = message.getHeaders();

			assertThat(headers.get("requestAttributes")).isEqualTo(attributes);

			Object requestParams = headers.get("requestParams");
			assertThat(requestParams).isNotNull();
			assertThat(((MultiValueMap<String, String>) requestParams).toSingleValueMap()).isEqualTo(params);

			Object matrixVariables = headers.get("matrixVariables");
			assertThat(matrixVariables).isInstanceOf(Map.class);
			Object value = ((Map<?, ?>) matrixVariables).get("value");
			assertThat(value).isInstanceOf(MultiValueMap.class);
			assertThat(((MultiValueMap<String, ?>) value).getFirst("q1")).isEqualTo("1");
			assertThat(((MultiValueMap<String, ?>) value).getFirst("q2")).isEqualTo("2");

			Object requestHeaders = headers.get("requestHeaders");
			assertThat(requestParams).isNotNull();
			assertThat(((HttpHeaders) requestHeaders).getContentType()).isEqualTo(MediaType.TEXT_PLAIN);

			MultiValueMap<String, Cookie> cookies = (MultiValueMap<String, Cookie>) headers.get("cookies");
			assertThat(cookies.size()).isEqualTo(1);
			Cookie foo = cookies.getFirst("foo");
			assertThat(foo).isNotNull();
			assertThat(foo).isEqualTo(cookie);
		});

		MockHttpServletResponse response = new MockHttpServletResponse();

		Object handler = this.handlerMapping.getHandler(request).getHandler();
		this.handlerAdapter.handle(request, response, handler);
		final String testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo(testRequest.split(";")[0].toLowerCase());

		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void testParams() throws Exception {
		// There is no matching handlers and some default handler
		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleNoMatch
		assertThatExceptionOfType(UnsatisfiedServletRequestParameterException.class)
				.isThrownBy(() -> this.handlerMapping.getHandler(new MockHttpServletRequest("GET", "/params")));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/params");
		request.addParameter("param1", "1");
		request.addParameter("param2", "1");

		Object handler = this.handlerMapping.getHandler(request).getHandler();

		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		String testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo("User=1;account=1");

		request = new MockHttpServletRequest("GET", "/params");
		request.addParameter("param1", "1");
		handler = this.handlerMapping.getHandler(request).getHandler();

		response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo("User=1");
	}

	@Test
	public void testConsumes() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consumes");
		request.setContentType("text/plain");
		Object handler = this.handlerMapping.getHandler(request).getHandler();
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		String testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo("BAR");

		request = new MockHttpServletRequest("GET", "/consumes");
		request.setContentType("text/xml");
		handler = this.handlerMapping.getHandler(request).getHandler();
		response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo("<test>TEXT_XML</test>");
	}

	@Test
	public void testProduces() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/produces");
		request.addHeader("Accept", "application/xml");
		Object handler = this.handlerMapping.getHandler(request).getHandler();

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		assertThat(request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE))
				.isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));

		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		String testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo("<test>XML</test>");

		request = new MockHttpServletRequest("GET", "/produces");
		request.addHeader("Accept", "application/json");
		handler = this.handlerMapping.getHandler(request).getHandler();

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		assertThat(request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE))
				.as("Negated expression should not be listed as a producible type").isNull();

		response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		testResponse = response.getContentAsString();
		assertThat(testResponse).isEqualTo("{\"json\":\"body\"}");
	}

}
