/*
 * Copyright 2017-2018 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.http.HttpMethod;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class WebFluxOutboundGatewayParserTests {

	@Autowired
	@Qualifier("reactiveMinimalConfig")
	private AbstractEndpoint reactiveMinimalConfigEndpoint;

	@Autowired
	@Qualifier("reactiveFullConfig")
	private AbstractEndpoint reactiveFullConfigEndpoint;

	@Autowired
	private WebClient webClient;

	@Autowired
	private BodyExtractor<?, ?> bodyExtractor;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void reactiveMinimalConfig() {
		Object handler = new DirectFieldAccessor(this.reactiveMinimalConfigEndpoint).getPropertyValue("handler");
		Object requestChannel = new DirectFieldAccessor(this.reactiveMinimalConfigEndpoint)
				.getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNull(replyChannel);
		assertSame(this.webClient, handlerAccessor.getPropertyValue("webClient"));
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test1", uriExpression.getValue());
		assertEquals(HttpMethod.POST.name(),
				TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals(Charset.forName("UTF-8"), handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));
		assertEquals(false, handlerAccessor.getPropertyValue("transferCookies"));
		assertEquals(false, handlerAccessor.getPropertyValue("replyPayloadToFlux"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reactiveFullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveFullConfigEndpoint);
		Object handler = endpointAccessor.getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.reactiveFullConfigEndpoint).getPropertyValue("inputChannel");
		assertEquals(this.applicationContext.getBean("requests"), requestChannel);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(77, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.FALSE, endpointAccessor.getPropertyValue("autoStartup"));
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertNotNull(replyChannel);
		assertEquals(this.applicationContext.getBean("replies"), replyChannel);

		assertEquals(String.class.getName(),
				TestUtils.getPropertyValue(handler, "expectedResponseTypeExpression", Expression.class).getValue());
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test2", uriExpression.getValue());
		assertEquals(HttpMethod.PUT.name(),
				TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals(Charset.forName("UTF-8"), handlerAccessor.getPropertyValue("charset"));
		assertEquals(false, handlerAccessor.getPropertyValue("extractPayload"));
		Object sendTimeout = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout");
		assertEquals(new Long("1234"), sendTimeout);
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertEquals(1, uriVariableExpressions.size());
		assertEquals("headers.bar", uriVariableExpressions.get("foo").getExpressionString());
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertEquals(2, mappedRequestHeaders.length);
		assertEquals(1, mappedResponseHeaders.length);
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1"));
		assertTrue(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2"));
		assertEquals("responseHeader", mappedResponseHeaders[0]);
		assertEquals(true, handlerAccessor.getPropertyValue("transferCookies"));
		assertEquals(true, handlerAccessor.getPropertyValue("replyPayloadToFlux"));
		assertSame(this.bodyExtractor, handlerAccessor.getPropertyValue("bodyExtractor"));
	}

}
