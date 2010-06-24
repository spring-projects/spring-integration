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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.core.MessageChannel;
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
public class HttpOutboundGatewayParserTests {

	@Autowired @Qualifier("minimalConfig")
	private AbstractEndpoint minimalConfigEndpoint;

	@Autowired @Qualifier("fullConfig")
	private AbstractEndpoint fullConfigEndpoint;

	@Autowired
	private ApplicationContext applicationContext;


	@Test
	public void minimalConfig() {
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) new DirectFieldAccessor(
				this.minimalConfigEndpoint).getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.minimalConfigEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNull(replyChannel);
		OutboundRequestMapper mapper = (OutboundRequestMapper) handlerAccessor.getPropertyValue("requestMapper");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertEquals("http://localhost/test1", handlerAccessor.getPropertyValue("uri"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(true, mapperAccessor.getPropertyValue("extractPayload"));
	}

	@Test
	public void fullConfig() throws Exception {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfigEndpoint);
		HttpRequestExecutingMessageHandler handler = (HttpRequestExecutingMessageHandler) endpointAccessor.getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.fullConfigEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(77, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNotNull(replyChannel);
		assertEquals(this.applicationContext.getBean("replies"), replyChannel);
		OutboundRequestMapper mapper = (OutboundRequestMapper) handlerAccessor.getPropertyValue("requestMapper");
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("restTemplate"));
		ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)
				templateAccessor.getPropertyValue("requestFactory");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertEquals("http://localhost/test2", handlerAccessor.getPropertyValue("uri"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(false, mapperAccessor.getPropertyValue("extractPayload"));
		Object requestFactoryBean = this.applicationContext.getBean("testRequestFactory");
		assertEquals(requestFactoryBean, requestFactory);
		Object sendTimeout = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("channelTemplate")).getPropertyValue("sendTimeout");
		assertEquals(new Long("1234"), sendTimeout);
	}

}
