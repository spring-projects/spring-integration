/*
 * Copyright 2015-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Artem Bilan
 *
 * @since 4.2
 */
@SpringJUnitConfig
@DirtiesContext
public class CrossOriginTests {

	@Autowired
	private IntegrationRequestMappingHandlerMapping handlerMapping;

	private MockHttpServletRequest request;

	@BeforeEach
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.request.setMethod("GET");
		this.request.addHeader(HttpHeaders.ORIGIN, "https://domain.com/");
	}

	@Test
	public void noEndpointWithoutOriginHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertThat(config).isNull();
	}

	@Test
	public void noEndpointWithOriginHeader() throws Exception {
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertThat(config).isNull();
	}

	@Test
	public void noEndpointPostWithOriginHeader() throws Exception {
		this.request.setMethod("POST");
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertThat(config).isNull();
	}

	@Test
	public void defaultEndpointWithCrossOrigin() throws Exception {
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertThat(config).isNotNull();
		assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[] { "GET" });
		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowCredentials()).isTrue();
		assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getExposedHeaders()).isEmpty();
		assertThat(config.getMaxAge()).isEqualTo(1800L);
	}

	@Test
	public void customized() throws Exception {
		this.request.setRequestURI("/customized");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertThat(config).isNotNull();
		assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[] { "DELETE" });
		assertThat(config.getAllowedOrigins().toArray())
				.isEqualTo(new String[] { "https://site1.com", "https://site2.com" });
		assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[] { "header1", "header2" });
		assertThat(config.getExposedHeaders().toArray()).isEqualTo(new String[] { "header3", "header4" });
		assertThat(config.getMaxAge()).isEqualTo(123L);
		assertThat(config.getAllowCredentials()).isEqualTo(false);
	}

	@Test
	public void preFlightRequest() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertThat(config).isNotNull();
		assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[] { "GET" });
		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowCredentials()).isTrue();
		assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getExposedHeaders()).isEmpty();
		assertThat(config.getMaxAge()).isEqualTo(1800L);
	}

	@Test
	public void ambiguousHeaderPreFlightRequest() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		this.request.setRequestURI("/ambiguous-header");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertThat(config).isNotNull();
		assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowCredentials()).isTrue();
		assertThat(config.getExposedHeaders()).isNull();
		assertThat(config.getMaxAge()).isNull();
	}

	@Test
	public void ambiguousProducesPreFlightRequest() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/ambiguous-produces");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(this.request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertThat(config).isNotNull();
		assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[] { "*" });
		assertThat(config.getAllowCredentials()).isTrue();
		assertThat(config.getExposedHeaders()).isNull();
		assertThat(config.getMaxAge()).isNull();
	}

	@Test
	public void testOptionsHeaderHandling() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/default");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		HandlerExecutionChain handler = this.handlerMapping.getHandler(request);
		assertThat(handler).isNotNull();
		Object handlerMethod = handler.getHandler();
		assertThat(handlerMethod).isNotNull();
		assertThat(handlerMethod).isInstanceOf(HandlerMethod.class);
		assertThat(((HandlerMethod) handlerMethod).getBeanType().getName()).contains("HttpOptionsHandler");
	}

	private CorsConfiguration getCorsConfiguration(HandlerExecutionChain chain, boolean isPreFlightRequest) {
		if (isPreFlightRequest) {
			Object handler = chain.getHandler();
			assertThat(handler.getClass().getSimpleName().equals("PreFlightHandler")).isTrue();
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
