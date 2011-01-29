/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http.config;

import static junit.framework.Assert.assertNotSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpOutboundChannelAdapterParserTests {

	@Autowired @Qualifier("minimalConfig")
	private AbstractEndpoint minimalConfigEndpoint;

	@Autowired @Qualifier("fullConfig")
	private AbstractEndpoint fullConfigEndpoint;
	
	@Autowired @Qualifier("restTemplateConfig")
	private AbstractEndpoint restTemplateConfig;

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private RestTemplate restTemplate;


	@Test
	public void minimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.minimalConfigEndpoint);
		RestTemplate rTemplate = 
			TestUtils.getPropertyValue(this.minimalConfigEndpoint, "handler.restTemplate", RestTemplate.class);
		assertNotSame(restTemplate, rTemplate);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		assertEquals("http://localhost/test1", handlerAccessor.getPropertyValue("uri"));
		assertEquals(HttpMethod.POST, handlerAccessor.getPropertyValue("httpMethod"));
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfigEndpoint);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		assertEquals(77, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertEquals(Boolean.class, handlerAccessor.getPropertyValue("expectedResponseType"));
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Object converterListBean = this.applicationContext.getBean("converterList");
		assertEquals(converterListBean, templateAccessor.getPropertyValue("messageConverters"));
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertEquals(requestFactoryBean, requestFactory);
		Object errorHandlerBean = this.applicationContext.getBean("testErrorHandler");
		assertEquals(errorHandlerBean, templateAccessor.getPropertyValue("errorHandler"));
		assertEquals("http://localhost/test2/{foo}", handlerAccessor.getPropertyValue("uri"));
		assertEquals(HttpMethod.GET, handlerAccessor.getPropertyValue("httpMethod"));
		assertEquals("UTF-8", handlerAccessor.getPropertyValue("charset"));
		assertEquals(false, handlerAccessor.getPropertyValue("extractPayload"));
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertEquals(1, uriVariableExpressions.size());
		assertEquals("headers.bar", uriVariableExpressions.get("foo").getExpressionString());
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertEquals(2, mappedRequestHeaders.length);
		assertEquals(0, mappedResponseHeaders.length);
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1"));
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2"));
	}
	
	@Test
	public void restTemplateConfig() {
		RestTemplate rTemplate = 
			TestUtils.getPropertyValue(this.restTemplateConfig, "handler.restTemplate", RestTemplate.class);
		assertEquals(restTemplate, rTemplate);
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void failWithRestTemplateAndRestAttributes() {
		new ClassPathXmlApplicationContext("HttpOutboundChannelAdapterParserTests-fail-context.xml", this.getClass());
	}


	public static class StubErrorHandler implements ResponseErrorHandler {

		public boolean hasError(ClientHttpResponse response) throws IOException {
			return false;
		}

		public void handleError(ClientHttpResponse response) throws IOException {
		}
	}

}
