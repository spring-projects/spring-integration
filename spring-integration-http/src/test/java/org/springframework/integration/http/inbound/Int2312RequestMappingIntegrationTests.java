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

package org.springframework.integration.http.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
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
//INT-2312
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
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
		assertEquals(TEST_STRING_MULTIPLE_PATHS, response.getContentAsString());

		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/path2");
		response = new MockHttpServletResponse();
		handler = this.handlerMapping.getHandler(request).getHandler();
		this.handlerAdapter.handle(request, response, handler);
		assertEquals(TEST_STRING_MULTIPLE_PATHS, response.getContentAsString());
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
		final Map<String, String> params = new HashMap<String, String>();
		params.put("foo", "bar");
		request.setParameters(params);
		request.setContent("hello".getBytes());
		final Cookie cookie = new Cookie("foo", "bar");
		request.setCookies(cookie);
		request.addHeader("toLowerCase", true);

		//See org.springframework.web.servlet.FrameworkServlet#initContextHolders
		final RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);

		this.toLowerCaseChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageHeaders headers = message.getHeaders();

				assertEquals(attributes, headers.get("requestAttributes"));

				Object requestParams = headers.get("requestParams");
				assertNotNull(requestParams);
				assertEquals(params, ((MultiValueMap<String, String>) requestParams).toSingleValueMap());

				Object matrixVariables = headers.get("matrixVariables");
				assertThat(matrixVariables, Matchers.instanceOf(Map.class));
				Object value = ((Map) matrixVariables).get("value");
				assertThat(value, Matchers.instanceOf(MultiValueMap.class));
				assertEquals("1", ((MultiValueMap) value).getFirst("q1"));
				assertEquals("2", ((MultiValueMap) value).getFirst("q2"));

				Object requestHeaders = headers.get("requestHeaders");
				assertNotNull(requestParams);
				assertEquals(MediaType.TEXT_PLAIN, ((HttpHeaders) requestHeaders).getContentType());

				Map<String, Cookie> cookies = (Map<String, Cookie>) headers.get("cookies");
				assertEquals(1, cookies.size());
				Cookie foo = cookies.get("foo");
				assertNotNull(foo);
				assertEquals(cookie, foo);
			}
		});

		MockHttpServletResponse response = new MockHttpServletResponse();

		Object handler = this.handlerMapping.getHandler(request).getHandler();
		this.handlerAdapter.handle(request, response, handler);
		final String testResponse = response.getContentAsString();
		assertEquals(testRequest.split(";")[0].toLowerCase(), testResponse);

		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void testParams() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/params");
        Object handler = null;
        try {
            handler = this.handlerMapping.getHandler(request);
        }
        catch (Exception e) {
            // There is no matching handlers and some default handler
            //See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleNoMatch
            assertTrue(e instanceof UnsatisfiedServletRequestParameterException);
        }

		request = new MockHttpServletRequest("GET", "/params");
		request.addParameter("param1", "1");
		request.addParameter("param2", "1");

		handler = this.handlerMapping.getHandler(request).getHandler();

		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		String testResponse = response.getContentAsString();
		assertEquals("User=1;account=1", testResponse);

		request = new MockHttpServletRequest("GET", "/params");
		request.addParameter("param1", "1");
		handler = this.handlerMapping.getHandler(request).getHandler();

		response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		testResponse = response.getContentAsString();
		assertEquals("User=1", testResponse);
	}

	@Test
	public void testConsumes() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consumes");
		request.setContentType("text/plain");
		Object handler = this.handlerMapping.getHandler(request).getHandler();
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		String testResponse = response.getContentAsString();
		assertEquals("BAR", testResponse);

		request = new MockHttpServletRequest("GET", "/consumes");
		request.setContentType("text/xml");
		handler = this.handlerMapping.getHandler(request).getHandler();
		response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		testResponse = response.getContentAsString();
		assertEquals("<test>TEXT_XML</test>", testResponse);
	}

	@Test
	public void testProduces() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/produces");
		request.addHeader("Accept", "application/xml");
		Object handler = this.handlerMapping.getHandler(request).getHandler();

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		assertEquals(Collections.singleton(MediaType.APPLICATION_XML),
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE));

		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		String testResponse = response.getContentAsString();
		assertEquals("<test>XML</test>", testResponse);

		request = new MockHttpServletRequest("GET", "/produces");
		request.addHeader("Accept", "application/json");
		handler = this.handlerMapping.getHandler(request).getHandler();

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		assertNull("Negated expression should not be listed as a producible type",
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE));

		response = new MockHttpServletResponse();

		this.handlerAdapter.handle(request, response, handler);
		testResponse = response.getContentAsString();
		assertEquals("{\"json\":\"body\"}", testResponse);
	}

}
