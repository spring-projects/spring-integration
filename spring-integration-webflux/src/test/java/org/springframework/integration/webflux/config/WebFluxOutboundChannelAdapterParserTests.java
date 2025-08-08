/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
		WebClient webClient =
				TestUtils.getPropertyValue(this.reactiveMinimalConfig, "handler.webClient", WebClient.class);
		assertThat(webClient).isNotSameAs(this.webClient);
		Object handler = endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("expectReply")).isEqualTo(false);
		assertThat(endpointAccessor.getPropertyValue("inputChannel"))
				.isEqualTo(this.applicationContext.getBean("requests"));
		assertThat(handlerAccessor.getPropertyValue("outputChannel")).isNull();
		Expression uriExpression = (Expression) handlerAccessor.getPropertyValue("uriExpression");
		assertThat(uriExpression.getValue()).isEqualTo("http://localhost/test1");
		assertThat(TestUtils.getPropertyValue(handler, "httpMethodExpression", Expression.class).getExpressionString())
				.isEqualTo(HttpMethod.POST.name());
		assertThat(handlerAccessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(handlerAccessor.getPropertyValue("extractPayload")).isEqualTo(true);
		assertThat(
				TestUtils.getPropertyValue(handler, "webClient.uriBuilderFactory.encodingMode",
						DefaultUriBuilderFactory.EncodingMode.class))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
	}

	@Test
	public void reactiveWebClientConfig() {
		assertThat(TestUtils.getPropertyValue(this.reactiveWebClientConfig, "handler.webClient"))
				.isSameAs(this.webClient);
		assertThat(TestUtils.getPropertyValue(this.reactiveWebClientConfig,
				"handler.publisherElementTypeExpression.value"))
				.isSameAs(Date.class);
	}

}
