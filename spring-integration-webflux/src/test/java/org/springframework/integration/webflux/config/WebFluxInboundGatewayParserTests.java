/*
 * Copyright 2018-2020 the original author or authors.
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
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

/**
 * @author Artem Bilan
 *
 * @since 5.0.1
 */
@SpringJUnitConfig
@DirtiesContext
public class WebFluxInboundGatewayParserTests {

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
		assertThat(endpointAccessor.getPropertyValue("requestChannel")).isSameAs(this.requests);
		assertThat((boolean) endpointAccessor.getPropertyValue("autoStartup")).isTrue();
		assertThat((boolean) endpointAccessor.getPropertyValue("expectReply")).isTrue();
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
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reactiveFullConfig() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(this.reactiveFullConfig);
		assertThat(endpointAccessor.getPropertyValue("requestChannel")).isSameAs(this.requests);
		assertThat(endpointAccessor.getPropertyValue("errorChannel")).isNotNull();
		assertThat((boolean) endpointAccessor.getPropertyValue("autoStartup")).isFalse();
		assertThat(endpointAccessor.getPropertyValue("phase")).isEqualTo(101);
		assertThat((boolean) endpointAccessor.getPropertyValue("expectReply")).isTrue();

		assertThat(((SpelExpression) endpointAccessor.getPropertyValue("statusCodeExpression")).getExpressionString())
				.isEqualTo("'504'");

		assertThat(((SpelExpression) endpointAccessor.getPropertyValue("payloadExpression")).getExpressionString())
				.isEqualTo("payload");

		Map<String, Expression> headerExpressions =
				(Map<String, Expression>) endpointAccessor.getPropertyValue("headerExpressions");

		assertThat(headerExpressions.containsKey("foo")).isTrue();

		assertThat(headerExpressions.get("foo").getValue()).isEqualTo("foo");

		CrossOrigin crossOrigin = (CrossOrigin) endpointAccessor.getPropertyValue("crossOrigin");
		assertThat(crossOrigin).isNotNull();
		assertThat(crossOrigin.getOrigin()).isEqualTo(new String[]{ "foo" });

		assertThat(endpointAccessor.getPropertyValue("requestPayloadType"))
				.isEqualTo(ResolvableType.forClass(byte[].class));

		assertThat(endpointAccessor.getPropertyValue("headerMapper")).isSameAs(this.headerMapper);
		assertThat(endpointAccessor.getPropertyValue("codecConfigurer")).isSameAs(this.serverCodecConfigurer);
		assertThat(endpointAccessor.getPropertyValue("requestedContentTypeResolver"))
				.isSameAs(this.requestedContentTypeResolver);
		assertThat(endpointAccessor.getPropertyValue("adapterRegistry")).isSameAs(this.reactiveAdapterRegistry);
	}

}
