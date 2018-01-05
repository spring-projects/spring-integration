/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.webflux.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.integration.http.inbound.CrossOrigin;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

/**
 * @author Artem Bilan
 *
 * @since 5.0.1
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class WebFluxInboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("reactiveMinimalConfig")
	private WebFluxInboundEndpoint reactiveMinimalConfig;

	@Autowired
	@Qualifier("reactiveFullConfig")
	private WebFluxInboundEndpoint reactiveFullConfig;

	@Autowired
	private MessageChannel requests;

	@Autowired
	private HeaderMapper<?> headerMapper;

	@Autowired
	private ServerCodecConfigurer serverCodecConfigurer;

	@Autowired
	private RequestedContentTypeResolver requestedContentTypeResolver;

	@Autowired
	private ReactiveAdapterRegistry reactiveAdapterRegistry;

	@Test
	public void reactiveMinimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveMinimalConfig);
		assertSame(this.requests, endpointAccessor.getPropertyValue("requestChannel"));
		assertTrue((boolean) endpointAccessor.getPropertyValue("autoStartup"));
		assertFalse((boolean) endpointAccessor.getPropertyValue("expectReply"));
		assertNull(endpointAccessor.getPropertyValue("statusCodeExpression"));
		assertNull(endpointAccessor.getPropertyValue("payloadExpression"));
		assertNull(endpointAccessor.getPropertyValue("headerExpressions"));
		assertNull(endpointAccessor.getPropertyValue("crossOrigin"));
		assertNull(endpointAccessor.getPropertyValue("requestPayloadType"));

		assertNotSame(this.headerMapper, endpointAccessor.getPropertyValue("headerMapper"));
		assertNotSame(this.serverCodecConfigurer, endpointAccessor.getPropertyValue("codecConfigurer"));
		assertNotSame(this.requestedContentTypeResolver, endpointAccessor.getPropertyValue("requestedContentTypeResolver"));
		assertNotSame(this.reactiveAdapterRegistry, endpointAccessor.getPropertyValue("adapterRegistry"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reactiveFullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveFullConfig);
		assertSame(this.requests, endpointAccessor.getPropertyValue("requestChannel"));
		assertNotNull(endpointAccessor.getPropertyValue("errorChannel"));
		assertFalse((boolean) endpointAccessor.getPropertyValue("autoStartup"));
		assertEquals(101, endpointAccessor.getPropertyValue("phase"));
		assertFalse((boolean) endpointAccessor.getPropertyValue("expectReply"));

		assertEquals("'202'",
				((SpelExpression) endpointAccessor.getPropertyValue("statusCodeExpression")).getExpressionString());

		assertEquals("payload",
				((SpelExpression) endpointAccessor.getPropertyValue("payloadExpression")).getExpressionString());

		Map<String, Expression> headerExpressions =
				(Map<String, Expression>) endpointAccessor.getPropertyValue("headerExpressions");

		assertTrue(headerExpressions.containsKey("foo"));

		assertEquals("foo", headerExpressions.get("foo").getValue());

		CrossOrigin crossOrigin = (CrossOrigin) endpointAccessor.getPropertyValue("crossOrigin");
		assertNotNull(crossOrigin);
		assertArrayEquals(new String[] { "foo" }, crossOrigin.getOrigin());

		assertEquals(ResolvableType.forClass(byte[].class), endpointAccessor.getPropertyValue("requestPayloadType"));

		assertSame(this.headerMapper, endpointAccessor.getPropertyValue("headerMapper"));
		assertSame(this.serverCodecConfigurer, endpointAccessor.getPropertyValue("codecConfigurer"));
		assertSame(this.requestedContentTypeResolver, endpointAccessor.getPropertyValue("requestedContentTypeResolver"));
		assertSame(this.reactiveAdapterRegistry, endpointAccessor.getPropertyValue("adapterRegistry"));
	}

}
