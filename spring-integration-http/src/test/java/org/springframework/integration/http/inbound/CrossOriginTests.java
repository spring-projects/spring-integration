/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Artem Bilan
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class CrossOriginTests {

	@Autowired
	private IntegrationRequestMappingHandlerMapping handlerMapping;

	private MockHttpServletRequest request;

	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.request.setMethod("GET");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain.com/");
	}

	@Test
	public void noEndpointWithoutOriginHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNull(config);
	}

	@Test
	public void noEndpointWithOriginHeader() throws Exception {
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNull(config);
	}

	@Test
	public void noEndpointPostWithOriginHeader() throws Exception {
		this.request.setMethod("POST");
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNull(config);
	}

	@Test
	public void defaultEndpointWithCrossOrigin() throws Exception {
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] { "GET" }, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] { "*" }, config.getAllowedOrigins().toArray());
		assertTrue(config.getAllowCredentials());
		assertArrayEquals(new String[] { "*" }, config.getAllowedHeaders().toArray());
		assertNull(config.getExposedHeaders());
		assertEquals(new Long(1800), config.getMaxAge());
	}

	@Test
	public void customized() throws Exception {
		this.request.setRequestURI("/customized");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] { "DELETE" }, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] { "http://site1.com", "http://site2.com" }, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[] { "header1", "header2" }, config.getAllowedHeaders().toArray());
		assertArrayEquals(new String[] { "header3", "header4" }, config.getExposedHeaders().toArray());
		assertEquals(new Long(123), config.getMaxAge());
		assertEquals(false, config.getAllowCredentials());
	}

	@Test
	public void preFlightRequest() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[] { "GET" }, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] { "*" }, config.getAllowedOrigins().toArray());
		assertTrue(config.getAllowCredentials());
		assertArrayEquals(new String[] { "*" }, config.getAllowedHeaders().toArray());
		assertNull(config.getExposedHeaders());
		assertEquals(new Long(1800), config.getMaxAge());
	}

	@Test
	public void ambiguousHeaderPreFlightRequest() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		this.request.setRequestURI("/ambiguous-header");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[] { "*" }, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] { "*" }, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[] { "*" }, config.getAllowedHeaders().toArray());
		assertTrue(config.getAllowCredentials());
		assertNull(config.getExposedHeaders());
		assertNull(config.getMaxAge());
	}

	@Test
	public void ambiguousProducesPreFlightRequest() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/ambiguous-produces");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[] { "*" }, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] { "*" }, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[] { "*" }, config.getAllowedHeaders().toArray());
		assertTrue(config.getAllowCredentials());
		assertNull(config.getExposedHeaders());
		assertNull(config.getMaxAge());
	}

	@Test
	public void testOptionsHeaderHandling() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/default");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		HandlerExecutionChain handler = this.handlerMapping.getHandler(request);
		assertNotNull(handler);
		Object handlerMethod = handler.getHandler();
		assertNotNull(handlerMethod);
		assertThat(handlerMethod, instanceOf(HandlerMethod.class));
		assertThat(((HandlerMethod) handlerMethod).getBeanType().getName(),
				containsString("HttpOptionsHandler"));
	}

	private CorsConfiguration getCorsConfiguration(HandlerExecutionChain chain, boolean isPreFlightRequest) {
		if (isPreFlightRequest) {
			Object handler = chain.getHandler();
			assertTrue(handler.getClass().getSimpleName().equals("PreFlightHandler"));
			return TestUtils.getPropertyValue(handler, "config", CorsConfiguration.class);
		}
		else {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			if (interceptors != null) {
				for (HandlerInterceptor interceptor : interceptors) {
					if (interceptor.getClass().getSimpleName().equals("CorsInterceptor")) {
						return TestUtils.getPropertyValue(interceptor, "config", CorsConfiguration.class);
					}
				}
			}
		}
		return null;
	}

}
