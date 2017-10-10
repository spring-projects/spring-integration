/*
 * Copyright 2017 the original author or authors.
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.nio.charset.Charset;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class WebFluxOutboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("reactiveMinimalConfig")
	private AbstractEndpoint reactiveMinimalConfig;

	@Autowired
	@Qualifier("reactiveWebClientConfig")
	private AbstractEndpoint reactiveWebClientConfig;

	@Autowired
	private WebClient webClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void reactiveMinimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveMinimalConfig);
		WebClient webClient =
				TestUtils.getPropertyValue(this.reactiveMinimalConfig, "handler.webClient", WebClient.class);
		assertNotSame(this.webClient, webClient);
		Object handler = endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("expectReply"));
		assertEquals(this.applicationContext.getBean("requests"), endpointAccessor.getPropertyValue("inputChannel"));
		assertNull(handlerAccessor.getPropertyValue("outputChannel"));
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertEquals("http://localhost/test1", uriExpression.getValue());
		assertEquals(HttpMethod.POST.name(),
				TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString());
		assertEquals(Charset.forName("UTF-8"), handlerAccessor.getPropertyValue("charset"));
		assertEquals(true, handlerAccessor.getPropertyValue("extractPayload"));
	}

	@Test
	public void reactiveWebClientConfig() {
		assertSame(this.webClient, TestUtils.getPropertyValue(this.reactiveWebClientConfig, "handler.webClient"));
	}

}
