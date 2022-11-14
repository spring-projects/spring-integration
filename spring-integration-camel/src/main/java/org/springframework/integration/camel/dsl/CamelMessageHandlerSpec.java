/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.camel.dsl;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.LambdaRouteBuilder;

import org.springframework.expression.Expression;
import org.springframework.integration.camel.outbound.CamelMessageHandler;
import org.springframework.integration.camel.support.CamelHeaderMapper;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageHandlerSpec} for {@link CamelMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class CamelMessageHandlerSpec extends
		MessageHandlerSpec<CamelMessageHandlerSpec, CamelMessageHandler> {

	private String[] inboundHeaderNames = {"*"};

	private String[] outboundHeaderNames = {"*"};

	protected CamelMessageHandlerSpec(@Nullable ProducerTemplate producerTemplate) {
		this.target = producerTemplate == null ? new CamelMessageHandler() : new CamelMessageHandler(producerTemplate);
	}

	public CamelMessageHandlerSpec endpointUri(String endpointUri) {
		this.target.setEndpointUri(endpointUri);
		return this;
	}

	public CamelMessageHandlerSpec endpointUri(Function<Message<?>, String> endpointUriFunction) {
		return endpointUriExpression(new FunctionExpression<>(endpointUriFunction));
	}

	public CamelMessageHandlerSpec endpointUriExpression(String endpointUriExpression) {
		return endpointUriExpression(PARSER.parseExpression(endpointUriExpression));
	}

	public CamelMessageHandlerSpec endpointUriExpression(Expression endpointUriExpression) {
		this.target.setEndpointUriExpression(endpointUriExpression);
		return this;
	}

	protected CamelMessageHandlerSpec route(LambdaRouteBuilder route) {
		this.target.setRoute(route);
		return this;
	}

	public CamelMessageHandlerSpec exchangePattern(ExchangePattern exchangePattern) {
		this.target.setExchangePattern(exchangePattern);
		return this;
	}

	public CamelMessageHandlerSpec exchangePattern(Function<Message<?>, ExchangePattern> exchangePatternFunction) {
		return exchangePatternExpression(new FunctionExpression<>(exchangePatternFunction));
	}

	public CamelMessageHandlerSpec exchangePatternExpression(String exchangePatternExpression) {
		return exchangePatternExpression(PARSER.parseExpression(exchangePatternExpression));
	}

	public CamelMessageHandlerSpec exchangePatternExpression(Expression exchangePatternExpression) {
		this.target.setExchangePatternExpression(exchangePatternExpression);
		return this;
	}

	public CamelMessageHandlerSpec inboundHeaderNames(String... inboundHeaderNames) {
		Assert.notEmpty(inboundHeaderNames, "'inboundHeaderNames' must not be empty");
		this.inboundHeaderNames = Arrays.copyOf(inboundHeaderNames, inboundHeaderNames.length);
		return addCamelHeaderMapper();
	}

	public CamelMessageHandlerSpec outboundHeaderNames(String... outboundHeaderNames) {
		Assert.notEmpty(outboundHeaderNames, "'outboundHeaderNames' must not be empty");
		this.outboundHeaderNames = Arrays.copyOf(outboundHeaderNames, outboundHeaderNames.length);
		return addCamelHeaderMapper();
	}

	private CamelMessageHandlerSpec addCamelHeaderMapper() {
		CamelHeaderMapper headerMapper = new CamelHeaderMapper();
		headerMapper.setInboundHeaderNames(this.inboundHeaderNames);
		headerMapper.setOutboundHeaderNames(this.outboundHeaderNames);
		return headerMapper(headerMapper);
	}

	public CamelMessageHandlerSpec headerMapper(HeaderMapper<org.apache.camel.Message> headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	public CamelMessageHandlerSpec exchangeProperties(Map<String, Object> exchangeProperties) {
		this.target.setExchangeProperties(exchangeProperties);
		return this;
	}

	public CamelMessageHandlerSpec exchangePropertiesExpression(String exchangePropertiesExpression) {
		return exchangePropertiesExpression(PARSER.parseExpression(exchangePropertiesExpression));
	}

	public CamelMessageHandlerSpec exchangePropertiesExpression(Expression exchangePropertiesExpression) {
		this.target.setExchangePropertiesExpression(exchangePropertiesExpression);
		return this;
	}

}
