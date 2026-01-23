/*
 * Copyright 2017-present the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.http.HttpMethod;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.0
 */
@SpringJUnitConfig
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
		WebClient webClient = TestUtils.getPropertyValue(this.reactiveMinimalConfig, "handler.webClient");
		assertThat(webClient).isNotSameAs(this.webClient);
		Object handler = endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test1");
		assertThat(TestUtils.<Expression>getPropertyValue(handler, "httpMethodExpression").getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);
		assertThat(
				TestUtils.<DefaultUriBuilderFactory.EncodingMode>getPropertyValue(handler,
						"webClient.uriBuilderFactory.encodingMode"))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
	}

	@Test
	public void reactiveWebClientConfig() {
		assertThat(TestUtils.<Object>getPropertyValue(this.reactiveWebClientConfig, "handler.webClient"))
				.isSameAs(this.webClient);
		assertThat(TestUtils.<Object>getPropertyValue(this.reactiveWebClientConfig,
				"handler.publisherElementTypeExpression.value"))
				.isSameAs(Date.class);
	}

}
