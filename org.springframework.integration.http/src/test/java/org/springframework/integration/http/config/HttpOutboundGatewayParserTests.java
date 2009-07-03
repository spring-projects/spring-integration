/*
 * Copyright 2002-2009 the original author or authors.
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.http.DefaultOutboundRequestMapper;
import org.springframework.integration.http.HttpOutboundEndpoint;
import org.springframework.integration.http.HttpRequestExecutor;
import org.springframework.integration.http.OutboundRequestMapper;
import org.springframework.integration.http.SimpleHttpRequestExecutor;
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

	@Autowired @Qualifier("fullConfigWithMapper")
	private AbstractEndpoint fullConfigWithMapperEndpoint;

	@Autowired @Qualifier("fullConfigWithoutMapper")
	private AbstractEndpoint fullConfigWithoutMapperEndpoint;

	@Autowired
	private ApplicationContext applicationContext;


	@Test
	public void minimalConfig() {
		HttpOutboundEndpoint gateway = (HttpOutboundEndpoint) new DirectFieldAccessor(
				this.minimalConfigEndpoint).getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.minimalConfigEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object replyChannel = accessor.getPropertyValue("outputChannel");
		assertNull(replyChannel);
		OutboundRequestMapper mapper = (OutboundRequestMapper) accessor.getPropertyValue("requestMapper");
		HttpRequestExecutor executor = (HttpRequestExecutor) accessor.getPropertyValue("requestExecutor");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(executor instanceof SimpleHttpRequestExecutor);
		Object mapperBean = this.applicationContext.getBean("mapper");
		assertNotSame(mapperBean, mapper);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertNull(mapperAccessor.getPropertyValue("defaultUrl"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(true, mapperAccessor.getPropertyValue("extractPayload"));
	}

	@Test
	public void fullConfigWithMapper() throws Exception {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.fullConfigWithMapperEndpoint);
		HttpOutboundEndpoint gateway = (HttpOutboundEndpoint) endpointAccessor.getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.fullConfigWithMapperEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(77, accessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		Object replyChannel = accessor.getPropertyValue("outputChannel");
		assertNotNull(replyChannel);
		assertEquals(this.applicationContext.getBean("replies"), replyChannel);
		OutboundRequestMapper mapper = (OutboundRequestMapper) accessor.getPropertyValue("requestMapper");
		HttpRequestExecutor executor = (HttpRequestExecutor) accessor.getPropertyValue("requestExecutor");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(executor instanceof SimpleHttpRequestExecutor);
		Object mapperBean = this.applicationContext.getBean("mapper");
		assertEquals(mapperBean, mapper);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertEquals(new URL("http://localhost/test"), mapperAccessor.getPropertyValue("defaultUrl"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(false, mapperAccessor.getPropertyValue("extractPayload"));
		Object executorBean = this.applicationContext.getBean("executor");
		assertEquals(executorBean, executor);
		Object sendTimeout = new DirectFieldAccessor(
				accessor.getPropertyValue("channelTemplate")).getPropertyValue("sendTimeout");
		assertEquals(new Long("1234"), sendTimeout);
	}

	@Test
	public void fullConfigWithoutMapper() throws Exception {
		HttpOutboundEndpoint gateway = (HttpOutboundEndpoint) new DirectFieldAccessor(
				this.fullConfigWithoutMapperEndpoint).getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.fullConfigWithoutMapperEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object replyChannel = accessor.getPropertyValue("outputChannel");
		assertNotNull(replyChannel);
		assertEquals(this.applicationContext.getBean("replies"), replyChannel);
		OutboundRequestMapper mapper = (OutboundRequestMapper) accessor.getPropertyValue("requestMapper");
		HttpRequestExecutor executor = (HttpRequestExecutor) accessor.getPropertyValue("requestExecutor");
		assertTrue(mapper instanceof DefaultOutboundRequestMapper);
		assertTrue(executor instanceof SimpleHttpRequestExecutor);
		Object mapperBean = this.applicationContext.getBean("mapper");
		assertNotSame(mapperBean, mapper);
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(mapper);
		assertEquals(new URL("http://localhost/test"), mapperAccessor.getPropertyValue("defaultUrl"));
		assertEquals("UTF-8", mapperAccessor.getPropertyValue("charset"));
		assertEquals(false, mapperAccessor.getPropertyValue("extractPayload"));
		Object executorBean = this.applicationContext.getBean("executor");
		assertEquals(executorBean, executor);
		Object sendTimeout = new DirectFieldAccessor(
				accessor.getPropertyValue("channelTemplate")).getPropertyValue("sendTimeout");
		assertEquals(new Long("1234"), sendTimeout);
	}	

}
