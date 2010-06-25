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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.http.DefaultOutboundRequestMapper;
import org.springframework.integration.http.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.OutboundRequestMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

	@Autowired
	private ApplicationContext applicationContext;


	@Test
	public void minimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.minimalConfigEndpoint);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		OutboundRequestMapper mapper = (OutboundRequestMapper) handlerAccessor.getPropertyValue("requestMapper");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertEquals("http://localhost/test1", handlerAccessor.getPropertyValue("uri"));
		assertEquals(HttpMethod.POST, handlerAccessor.getPropertyValue("httpMethod"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(true, mapperAccessor.getPropertyValue("extractPayload"));
	}

	@Test
	public void fullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfigEndpoint);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		assertEquals(77, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		OutboundRequestMapper mapper = (OutboundRequestMapper) handlerAccessor.getPropertyValue("requestMapper");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		Object converterListBean = this.applicationContext.getBean("converterList");
		assertEquals(converterListBean, templateAccessor.getPropertyValue("messageConverters"));
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertEquals(requestFactoryBean, requestFactory);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertEquals("http://localhost/test2", handlerAccessor.getPropertyValue("uri"));
		assertEquals(HttpMethod.GET, handlerAccessor.getPropertyValue("httpMethod"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(false, mapperAccessor.getPropertyValue("extractPayload"));
	}

}
