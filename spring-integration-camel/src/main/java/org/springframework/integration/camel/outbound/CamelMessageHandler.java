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

package org.springframework.integration.camel.outbound;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.LambdaRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.camel.support.CamelHeaderMapper;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link org.springframework.messaging.MessageHandler} for calling Apache Camel route
 * and produce (optionally) a reply.
 * <p>
 * In the async mode, the {@link ProducerTemplate#asyncSend(Endpoint, Exchange)} is used.
 * <p>
 * The request-reply behavior can be controlled via {@link ExchangePattern} configuration
 * or per message. By default, this handler works in an {@link ExchangePattern#InOnly} mode.
 * <p>
 * A default "mapping all headers" between Spring Integration and Apache Camel messages behavior
 * can be customized via {@link  #setHeaderMapper(HeaderMapper)} option.
 * <p>
 * The target Apache Camel endpoint to call can be determined by the {@link #endpointUriExpression}.
 * By default, a {@link ProducerTemplate#getDefaultEndpoint()} is used.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see CamelHeaderMapper
 */
public class CamelMessageHandler extends AbstractReplyProducingMessageHandler {

	private ProducerTemplate producerTemplate;

	private Expression exchangePatternExpression = new ValueExpression<>(ExchangePattern.InOnly);

	@Nullable
	private Expression endpointUriExpression;

	@Nullable
	private LambdaRouteBuilder route;

	private HeaderMapper<org.apache.camel.Message> headerMapper = new CamelHeaderMapper();

	@Nullable
	private Expression exchangePropertiesExpression;

	private StandardEvaluationContext evaluationContext;

	public CamelMessageHandler() {
	}

	public CamelMessageHandler(ProducerTemplate producerTemplate) {
		Assert.notNull(producerTemplate, "'producerTemplate' must not be null");
		this.producerTemplate = producerTemplate;
	}

	/**
	 * Set Camel route endpoint uri to send a message.
	 * Mutually exclusive with {@link #setEndpointUriExpression(Expression)} and {@link #setRoute(LambdaRouteBuilder)}.
	 * @param endpointUri the Camel route endpoint to send a message.
	 */
	public void setEndpointUri(String endpointUri) {
		Assert.hasText(endpointUri, "'endpointUri' must not be empty");
		setEndpointUriExpression(new LiteralExpression(endpointUri));
	}

	/**
	 * Set Camel route endpoint uri to send a message.
	 * Mutually exclusive with {@link #setEndpointUri(String)} and {@link #setRoute(LambdaRouteBuilder)}.
	 * @param endpointUriExpression the SpEL expression to determine a Camel route endpoint to send a message.
	 */
	public void setEndpointUriExpression(Expression endpointUriExpression) {
		Assert.notNull(endpointUriExpression, "'endpointUriExpression' must not be null");
		this.endpointUriExpression = endpointUriExpression;
	}

	/**
	 * Set a {@link LambdaRouteBuilder} to add an inline Camel route definition.
	 * Can be used as a lambda {@code rb -> rb.from("direct:inbound").bean(MyBean.class)}
	 * or reference to external instance.
	 * Mutually exclusive with {@link #setEndpointUri(String)} and {@link #setEndpointUriExpression(Expression)}.
	 * The endpoint to send a message is extracted from the target {@link RouteBuilder}.
	 * @param route the {@link LambdaRouteBuilder} to use.
	 */
	public void setRoute(LambdaRouteBuilder route) {
		Assert.notNull(route, "'route' must not be null");
		this.route = route;
	}

	public void setExchangePattern(ExchangePattern exchangePattern) {
		Assert.notNull(exchangePattern, "'exchangePattern' must not be null");
		setExchangePatternExpression(new ValueExpression<>(exchangePattern));
	}

	public void setExchangePatternExpression(Expression exchangePatternExpression) {
		Assert.notNull(exchangePatternExpression, "'exchangePatternExpression' must not be null");
		this.exchangePatternExpression = exchangePatternExpression;
	}

	/**
	 * Set a {@link HeaderMapper} to map request message headers into Apache Camel message headers and
	 * back if request-reply exchange pattern is used.
	 * @param headerMapper the {@link HeaderMapper} to use.
	 */
	public void setHeaderMapper(HeaderMapper<org.apache.camel.Message> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null");
		this.headerMapper = headerMapper;
	}

	public void setExchangeProperties(Map<String, Object> exchangeProperties) {
		Assert.notNull(exchangeProperties, "'exchangeProperties' must not be null");
		setExchangePropertiesExpression(new ValueExpression<>(exchangeProperties));
	}

	/**
	 * Set a SpEL expression to evaluate {@link org.apache.camel.Exchange} properties as a {@link Map}.
	 * @param exchangePropertiesExpression the expression for exchange properties.
	 */
	public void setExchangePropertiesExpression(Expression exchangePropertiesExpression) {
		this.exchangePropertiesExpression = exchangePropertiesExpression;
	}

	@Override
	protected final void doInit() {
		Assert.state(this.endpointUriExpression == null || this.route == null,
				"The 'endpointUri' option is mutually exclusive with 'route'");

		BeanFactory beanFactory = getBeanFactory();
		if (this.producerTemplate == null) { // NOSONAR
			this.producerTemplate = beanFactory.getBean(CamelContext.class).createProducerTemplate();
		}

		LambdaRouteBuilder lambdaRouteBuilder = this.route;
		if (lambdaRouteBuilder != null) {
			CamelContext camelContext = this.producerTemplate.getCamelContext();
			RouteBuilder routeBuilder =
					new RouteBuilder(camelContext) {

						@Override
						public void configure() throws Exception {
							lambdaRouteBuilder.accept(this);
						}

					};

			try {
				camelContext.addRoutes(routeBuilder);
			}
			catch (Exception ex) {
				throw new BeanInitializationException("Cannot load Camel route", ex);
			}

			RouteDefinition routeDefinition = routeBuilder.getRouteCollection().getRoutes().get(0);
			this.endpointUriExpression = new LiteralExpression(routeDefinition.getInput().getEndpointUri());
		}

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		ExchangePattern exchangePattern =
				this.exchangePatternExpression.getValue(this.evaluationContext, requestMessage, ExchangePattern.class);

		Assert.notNull(exchangePattern, "'exchangePatternExpression' must not evaluate to null");

		Endpoint endpoint = resolveEndpoint(requestMessage);
		Exchange exchange = prepareInExchange(endpoint, exchangePattern, requestMessage);

		if (isAsync()) {
			CompletableFuture<Exchange> result = this.producerTemplate.asyncSend(endpoint, exchange);
			return result.thenApply(resultExchange -> buildReply(exchangePattern, resultExchange));
		}
		else {
			Exchange result = this.producerTemplate.send(endpoint, exchange);
			return buildReply(exchangePattern, result);
		}
	}

	private Endpoint resolveEndpoint(Message<?> requestMessage) {
		String endpointUri =
				this.endpointUriExpression != null
						? this.endpointUriExpression.getValue(this.evaluationContext, requestMessage, String.class)
						: null;

		if (StringUtils.hasText(endpointUri)) {
			return this.producerTemplate.getCamelContext().getEndpoint(endpointUri);
		}
		else {
			return this.producerTemplate.getDefaultEndpoint();
		}
	}

	@SuppressWarnings("unchecked")
	private Exchange prepareInExchange(Endpoint endpoint, ExchangePattern exchangePattern, Message<?> requestMessage) {
		Exchange exchange = endpoint.createExchange(exchangePattern);

		Map<String, Object> exchangeProperties =
				this.exchangePropertiesExpression != null
						? this.exchangePropertiesExpression.getValue(this.evaluationContext, requestMessage, Map.class)
						: null;

		if (exchangeProperties != null) {
			for (Map.Entry<String, Object> property : exchangeProperties.entrySet()) {
				exchange.setProperty(property.getKey(), property.getValue());
			}
		}
		org.apache.camel.Message in = exchange.getIn();
		this.headerMapper.fromHeaders(requestMessage.getHeaders(), in);
		in.setBody(requestMessage.getPayload());
		return exchange;
	}

	@Nullable
	private AbstractIntegrationMessageBuilder<?> buildReply(ExchangePattern exchangePattern, Exchange result) {
		if (result.isFailed()) {
			throw CamelExecutionException.wrapCamelExecutionException(result, result.getException());
		}
		if (exchangePattern.isOutCapable()) {
			org.apache.camel.Message out = result.getMessage();
			return getMessageBuilderFactory()
					.withPayload(out.getBody())
					.copyHeaders(this.headerMapper.toHeaders(out));
		}
		else {
			return null;
		}
	}

}
