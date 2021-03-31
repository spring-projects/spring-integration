/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.integration.webflux.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
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
		assertThat(requestChannel).isEqualTo(this.applicationContext.getBean("requests"));
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertThat(replyChannel).isNull();
		assertThat(handlerAccessor.getPropertyValue("webClient")).isSameAs(this.webClient);
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test1");
		assertThat(TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);
		assertThat(handlerAccessor.getPropertyValue("transferCookies")).isEqualTo(false);
		assertThat(handlerAccessor.getPropertyValue("replyPayloadToFlux")).isEqualTo(false);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reactiveFullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveFullConfigEndpoint);
		Object handler = endpointAccessor.getPropertyValue("handler");
		MessageChannel requestChannel = (MessageChannel) new DirectFieldAccessor(
				this.reactiveFullConfigEndpoint).getPropertyValue("inputChannel");
		assertThat(requestChannel).isEqualTo(this.applicationContext.getBean("requests"));
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("order")).isEqualTo(77);
		assertThat(endpointAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object replyChannel = handlerAccessor.getPropertyValue("outputChannel");
		assertThat(replyChannel).isNotNull();
		assertThat(replyChannel).isEqualTo(this.applicationContext.getBean("replies"));

		assertThat(TestUtils.getPropertyValue(handler, "expectedResponseTypeExpression", Expression.class).getValue())
				.isEqualTo(String.class.getName());
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test2");
		assertThat(TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString())
				.isEqualTo(HttpMethod.PUT.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(false);
		Object sendTimeout = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout");
		assertThat(sendTimeout).isEqualTo(1234L);
		Map<String, Expression> uriVariableExpressions =
				(Map<String, Expression>) handlerAccessor.getPropertyValue("uriVariableExpressions");
		assertThat(uriVariableExpressions).hasSize(1);
		assertThat(uriVariableExpressions.get("foo").getExpressionString()).isEqualTo("headers.bar");
		DirectFieldAccessor mapperAccessor = new DirectFieldAccessor(handlerAccessor.getPropertyValue("headerMapper"));
		String[] mappedRequestHeaders = (String[]) mapperAccessor.getPropertyValue("outboundHeaderNames");
		String[] mappedResponseHeaders = (String[]) mapperAccessor.getPropertyValue("inboundHeaderNames");
		assertThat(mappedRequestHeaders).hasSize(2);
		assertThat(mappedResponseHeaders).hasSize(1);
		assertThat(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader1")).isTrue();
		assertThat(ObjectUtils.containsElement(mappedRequestHeaders, "requestHeader2")).isTrue();
		assertThat(mappedResponseHeaders[0]).isEqualTo("responseHeader");
		assertThat(handlerAccessor.getPropertyValue("transferCookies")).isEqualTo(true);
		assertThat(handlerAccessor.getPropertyValue("replyPayloadToFlux")).isEqualTo(true);
		assertThat(handlerAccessor.getPropertyValue("bodyExtractor")).isSameAs(this.bodyExtractor);
		assertThat(handlerAccessor.getPropertyValue("publisherElementTypeExpression.expression"))
				.isEqualTo("headers.elementType");
		assertThat(handlerAccessor.getPropertyValue("extractResponseBody"))
				.isEqualTo(false);
	}

}
