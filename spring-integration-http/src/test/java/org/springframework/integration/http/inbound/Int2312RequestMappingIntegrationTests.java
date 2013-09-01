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

package org.springframework.integration.http.inbound;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

/**
 * @author Artem Bilan
 * @since 3.0
 */
//INT-2312
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Int2312RequestMappingIntegrationTests {

	public static final String TEST_PATH = "/test/{value}";

	@Autowired
	private HandlerMapping handlerMapping;

	private HandlerAdapter handlerAdapter = new HttpRequestHandlerAdapter();

	@Test
	public void testURIVariablesAndHeaders() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		String testRequest = "aBc";
		String requestURI = "/test/" + testRequest;
		request.setRequestURI(requestURI);

		//See org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleMatch
		Map<String, String> uriTemplateVariables =
				new AntPathMatcher().extractUriTemplateVariables(TEST_PATH, requestURI);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addHeader("toLowerCase", true);

		Object handler = this.handlerMapping.getHandler(request).getHandler();
		this.handlerAdapter.handle(request, response, handler);
		final String testResponse = response.getContentAsString();
		assertEquals(testRequest.toLowerCase(), testResponse);
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
