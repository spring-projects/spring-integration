/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.webflux.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.0.1
 */
@SpringJUnitConfig
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

	@Autowired
	private Validator validator;

	@Test
	public void reactiveMinimalConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveMinimalConfig);
		assertThat(endpointAccessor.getPropertyValue("requestChannel")).isSameAs(this.requests);
		assertThat((boolean) endpointAccessor.getPropertyValue("autoStartup")).isTrue();
		assertThat((boolean) endpointAccessor.getPropertyValue("expectReply")).isFalse();
		assertThat(endpointAccessor.getPropertyValue("statusCodeExpression")).isNull();
		assertThat(endpointAccessor.getPropertyValue("payloadExpression")).isNull();
		assertThat(endpointAccessor.getPropertyValue("headerExpressions")).isNull();
		assertThat(endpointAccessor.getPropertyValue("crossOrigin")).isNull();
		assertThat(endpointAccessor.getPropertyValue("requestPayloadType")).isNull();

		assertThat(endpointAccessor.getPropertyValue("headerMapper")).isNotSameAs(this.headerMapper);
		assertThat(endpointAccessor.getPropertyValue("codecConfigurer")).isNotSameAs(this.serverCodecConfigurer);
		assertThat(endpointAccessor.getPropertyValue("requestedContentTypeResolver"))
				.isNotSameAs(this.requestedContentTypeResolver);
		assertThat(endpointAccessor.getPropertyValue("adapterRegistry")).isNotSameAs(this.reactiveAdapterRegistry);
		assertThat(endpointAccessor.getPropertyValue("validator")).isSameAs(this.validator);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reactiveFullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveFullConfig);
		assertThat(endpointAccessor.getPropertyValue("requestChannel")).isSameAs(this.requests);
		assertThat(endpointAccessor.getPropertyValue("errorChannel")).isNotNull();
		assertThat((boolean) endpointAccessor.getPropertyValue("autoStartup")).isFalse();
		assertThat(endpointAccessor.getPropertyValue("phase")).isEqualTo(101);
		assertThat((boolean) endpointAccessor.getPropertyValue("expectReply")).isFalse();

		assertThat(((SpelExpression) endpointAccessor.getPropertyValue("statusCodeExpression")).getExpressionString())
				.isEqualTo("'202'");

		assertThat(((SpelExpression) endpointAccessor.getPropertyValue("payloadExpression")).getExpressionString())
				.isEqualTo("payload");

		Map<String, Expression> headerExpressions =
				(Map<String, Expression>) endpointAccessor.getPropertyValue("headerExpressions");

		assertThat(headerExpressions.containsKey("foo")).isTrue();

		assertThat(headerExpressions.get("foo").getValue()).isEqualTo("foo");

		CrossOrigin crossOrigin = (CrossOrigin) endpointAccessor.getPropertyValue("crossOrigin");
		assertThat(crossOrigin).isNotNull();
		assertThat(crossOrigin.getOrigin()).isEqualTo(new String[] {"foo"});

		assertThat(endpointAccessor.getPropertyValue("requestPayloadType"))
				.isEqualTo(ResolvableType.forClass(byte[].class));

		assertThat(endpointAccessor.getPropertyValue("headerMapper")).isSameAs(this.headerMapper);
		assertThat(endpointAccessor.getPropertyValue("codecConfigurer")).isSameAs(this.serverCodecConfigurer);
		assertThat(endpointAccessor.getPropertyValue("requestedContentTypeResolver"))
				.isSameAs(this.requestedContentTypeResolver);
		assertThat(endpointAccessor.getPropertyValue("adapterRegistry")).isSameAs(this.reactiveAdapterRegistry);
	}

}
