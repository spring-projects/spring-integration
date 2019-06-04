/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.rsocket.dsl;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequesterMethodArgumentResolver;

/**
 * The {@link MessageHandlerSpec} implementation for the {@link RSocketOutboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketOutboundGatewaySpec extends MessageHandlerSpec<RSocketOutboundGatewaySpec, RSocketOutboundGateway> {

	RSocketOutboundGatewaySpec(Expression routeExpression) {
		this.target = new RSocketOutboundGateway(routeExpression);
	}

	/**
	 * Configure a {@link ClientRSocketConnector} for client side requests based on the connection
	 * provided by the {@link ClientRSocketConnector#getRSocketRequester()}.
	 * In case of server side, an {@link RSocketRequester} must be provided in the
	 * {@link RSocketRequesterMethodArgumentResolver#RSOCKET_REQUESTER_HEADER} header of request message.
	 * @param clientRSocketConnector the {@link ClientRSocketConnector} to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setClientRSocketConnector(ClientRSocketConnector)
	 */
	public RSocketOutboundGatewaySpec clientRSocketConnector(ClientRSocketConnector clientRSocketConnector) {
		this.target.setClientRSocketConnector(clientRSocketConnector);
		return this;
	}

	/**
	 * Configure a {@link RSocketOutboundGateway.Command} for RSocket request type.
	 * @param command the {@link RSocketOutboundGateway.Command} to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setCommand(RSocketOutboundGateway.Command)
	 */
	public RSocketOutboundGatewaySpec command(RSocketOutboundGateway.Command command) {
		return command(new ValueExpression<>(command));
	}

	/**
	 * Configure a {@code Function} to evaluate a {@link RSocketOutboundGateway.Command}
	 * for RSocket request type at runtime against a request message.
	 * @param commandFunction the {@code Function} to use.
	 * @param <P> the expected request message payload type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setCommandExpression(Expression)
	 */
	public <P> RSocketOutboundGatewaySpec command(Function<Message<P>, ?> commandFunction) {
		return command(new FunctionExpression<>(commandFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a {@link RSocketOutboundGateway.Command}
	 * for RSocket request type at runtime against a request message.
	 * @param commandExpression the SpEL expression to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setCommandExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec command(String commandExpression) {
		return command(PARSER.parseExpression(commandExpression));
	}

	/**
	 * Configure a SpEL expression to evaluate a {@link RSocketOutboundGateway.Command}
	 * for RSocket request type at runtime against a request message.
	 * @param commandExpression the SpEL expression to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setCommandExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec command(Expression commandExpression) {
		this.target.setCommandExpression(commandExpression);
		return this;
	}

	/**
	 * Configure a type for a request {@link Publisher} elements.
	 * @param publisherElementType the type of the request {@link Publisher} elements.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementType(Class)
	 */
	public RSocketOutboundGatewaySpec publisherElementType(Class<?> publisherElementType) {
		return publisherElementType(new ValueExpression<>(publisherElementType));
	}

	/**
	 * Configure a {@code Function} to evaluate a request {@link Publisher} elements type at runtime against
	 * a request message.
	 * @param publisherElementTypeFunction the {@code Function} to evaluate a type for the request
	 * {@link Publisher} elements.
	 * @param <P> the expected request message payload type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementTypeExpression(Expression)
	 */
	public <P> RSocketOutboundGatewaySpec publisherElementType(Function<Message<P>, ?> publisherElementTypeFunction) {
		return publisherElementType(new FunctionExpression<>(publisherElementTypeFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a request {@link Publisher} elements type at runtime against
	 * a request message.
	 * @param publisherElementTypeExpression the expression to evaluate a type for the request
	 * {@link Publisher} elements.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementTypeExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec publisherElementType(String publisherElementTypeExpression) {
		return publisherElementType(PARSER.parseExpression(publisherElementTypeExpression));
	}

	/**
	 * Configure a SpEL expression to evaluate a request {@link Publisher} elements type at runtime against
	 * a request message.
	 * @param publisherElementTypeExpression the expression to evaluate a type for the request
	 * {@link Publisher} elements.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementTypeExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec publisherElementType(Expression publisherElementTypeExpression) {
		this.target.setPublisherElementTypeExpression(publisherElementTypeExpression);
		return this;
	}

	/**
	 * Specify the expected response type for the RSocket response.
	 * @param expectedResponseType The expected type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setExpectedResponseType(Class)
	 */
	public RSocketOutboundGatewaySpec expectedResponseType(Class<?> expectedResponseType) {
		return expectedResponseType(new ValueExpression<>(expectedResponseType));
	}

	/**
	 * Specify the {@code Function} to determine the type for the RSocket response.
	 * @param expectedResponseTypeFunction The expected response type {@code Function}.
	 * @param <P> the expected request message payload type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setExpectedResponseTypeExpression(Expression)
	 */
	public <P> RSocketOutboundGatewaySpec expectedResponseType(Function<Message<P>, ?> expectedResponseTypeFunction) {
		return expectedResponseType(new FunctionExpression<>(expectedResponseTypeFunction));
	}

	/**
	 * Specify the {@link Expression} to determine the type for the RSocket response.
	 * @param expectedResponseTypeExpression The expected response type expression.
	 * @return the spec
	 * @see RSocketOutboundGateway#setExpectedResponseTypeExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec expectedResponseType(String expectedResponseTypeExpression) {
		return expectedResponseType(PARSER.parseExpression(expectedResponseTypeExpression));
	}

	/**
	 * Specify the {@link Expression} to determine the type for the RSocket response.
	 * @param expectedResponseTypeExpression The expected response type expression.
	 * @return the spec
	 * @see RSocketOutboundGateway#setExpectedResponseTypeExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec expectedResponseType(Expression expectedResponseTypeExpression) {
		this.target.setExpectedResponseTypeExpression(expectedResponseTypeExpression);
		return this;
	}

}
