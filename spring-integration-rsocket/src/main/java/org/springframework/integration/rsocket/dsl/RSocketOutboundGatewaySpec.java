/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.RSocketInteractionModel;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.util.MimeType;

/**
 * The {@link MessageHandlerSpec} implementation for the {@link RSocketOutboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketOutboundGatewaySpec extends MessageHandlerSpec<RSocketOutboundGatewaySpec, RSocketOutboundGateway> {

	protected RSocketOutboundGatewaySpec(String route, Object... routeVariables) {
		this.target = new RSocketOutboundGateway(route, routeVariables);
	}

	protected RSocketOutboundGatewaySpec(Expression routeExpression) {
		this.target = new RSocketOutboundGateway(routeExpression);
	}

	/**
	 * Configure a {@link ClientRSocketConnector} for client side requests based on the connection
	 * provided by the {@link ClientRSocketConnector#getRequester()}.
	 * @param clientRSocketConnector the {@link ClientRSocketConnector} to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setClientRSocketConnector(ClientRSocketConnector)
	 */
	public RSocketOutboundGatewaySpec clientRSocketConnector(ClientRSocketConnector clientRSocketConnector) {
		this.target.setClientRSocketConnector(clientRSocketConnector);
		return this;
	}

	/**
	 * Configure an {@link RSocketInteractionModel} for the RSocket request type.
	 * @param interactionModel the {@link RSocketInteractionModel} to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setInteractionModel(RSocketInteractionModel)
	 * @since 5.2.2
	 */
	public RSocketOutboundGatewaySpec interactionModel(RSocketInteractionModel interactionModel) {
		return interactionModel(new ValueExpression<>(interactionModel));
	}

	/**
	 * Configure a {@link Function} to evaluate an {@link RSocketInteractionModel}
	 * for the RSocket request type at runtime against a request message.
	 * @param interactionModelFunction the {@code Function} to use.
	 * @param <P> the expected request message payload type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setInteractionModelExpression(Expression)
	 * @since 5.2.2
	 */
	public <P> RSocketOutboundGatewaySpec interactionModel(Function<Message<P>, ?> interactionModelFunction) {
		return interactionModel(new FunctionExpression<>(interactionModelFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate an {@link RSocketInteractionModel}
	 * for the RSocket request type at runtime against a request message.
	 * @param interactionModelExpression the SpEL expression to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setInteractionModelExpression(Expression)
	 * @since 5.2.2
	 */
	public RSocketOutboundGatewaySpec interactionModel(String interactionModelExpression) {
		return interactionModel(PARSER.parseExpression(interactionModelExpression));
	}

	/**
	 * Configure a SpEL expression to evaluate an {@link RSocketInteractionModel}
	 * for the RSocket request type at runtime against a request message.
	 * @param interactionModelExpression the SpEL expression to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setInteractionModelExpression(Expression)
	 * @since 5.2.2
	 */
	public RSocketOutboundGatewaySpec interactionModel(Expression interactionModelExpression) {
		this.target.setInteractionModelExpression(interactionModelExpression);
		return this;
	}

	/**
	 * Configure a type for a request {@link org.reactivestreams.Publisher} elements.
	 * @param publisherElementType the type of the request {@link org.reactivestreams.Publisher} elements.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementType(Class)
	 */
	public RSocketOutboundGatewaySpec publisherElementType(Class<?> publisherElementType) {
		return publisherElementType(new ValueExpression<>(publisherElementType));
	}

	/**
	 * Configure a {@link Function} to evaluate a request {@link org.reactivestreams.Publisher}
	 * elements type at runtime against a request message.
	 * @param publisherElementTypeFunction the {@code Function} to evaluate a type for the request
	 * {@link org.reactivestreams.Publisher} elements.
	 * @param <P> the expected request message payload type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementTypeExpression(Expression)
	 */
	public <P> RSocketOutboundGatewaySpec publisherElementType(Function<Message<P>, ?> publisherElementTypeFunction) {
		return publisherElementType(new FunctionExpression<>(publisherElementTypeFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a request {@link org.reactivestreams.Publisher}
	 * elements type at runtime against a request message.
	 * @param publisherElementTypeExpression the expression to evaluate a type for the request
	 * {@link org.reactivestreams.Publisher} elements.
	 * @return the spec
	 * @see RSocketOutboundGateway#setPublisherElementTypeExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec publisherElementType(String publisherElementTypeExpression) {
		return publisherElementType(PARSER.parseExpression(publisherElementTypeExpression));
	}

	/**
	 * Configure a SpEL expression to evaluate a request {@link org.reactivestreams.Publisher}
	 * elements type at runtime against a request message.
	 * @param publisherElementTypeExpression the expression to evaluate a type for the request
	 * {@link org.reactivestreams.Publisher} elements.
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
	 * Specify the {@link Function} to determine the type for the RSocket response.
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
	 * Specify an {@link Expression} to determine the type for the RSocket response.
	 * @param expectedResponseTypeExpression The expected response type expression.
	 * @return the spec
	 * @see RSocketOutboundGateway#setExpectedResponseTypeExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec expectedResponseType(Expression expectedResponseTypeExpression) {
		this.target.setExpectedResponseTypeExpression(expectedResponseTypeExpression);
		return this;
	}

	/**
	 * Configure a {@link Function} to evaluate a metadata as a {@code Map<Object, MimeType>}
	 * for RSocket request against request message.
	 * @param metadataFunction the {@code Function} to use.
	 * @param <P> the expected request message payload type.
	 * @return the spec
	 * @see RSocketOutboundGateway#setMetadataExpression(Expression)
	 */
	public <P> RSocketOutboundGatewaySpec metadata(Function<Message<P>, Map<Object, MimeType>> metadataFunction) {
		return metadata(new FunctionExpression<>(metadataFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a metadata as a {@code Map<Object, MimeType>}
	 * for the RSocket request against request message.
	 * @param metadataExpression the SpEL expression to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setMetadataExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec metadata(String metadataExpression) {
		return metadata(PARSER.parseExpression(metadataExpression));
	}

	/**
	 * Configure a SpEL expression to evaluate a metadata as a {@code Map<Object, MimeType>}
	 * for the RSocket request type at runtime against a request message.
	 * @param metadataExpression the SpEL expression to use.
	 * @return the spec
	 * @see RSocketOutboundGateway#setMetadataExpression(Expression)
	 */
	public RSocketOutboundGatewaySpec metadata(Expression metadataExpression) {
		this.target.setMetadataExpression(metadataExpression);
		return this;
	}

}
