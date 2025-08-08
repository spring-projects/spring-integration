/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket.dsl;

import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;

/**
 * The RSocket components Factory.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public final class RSockets {

	/**
	 * Create an {@link RSocketOutboundGatewaySpec} builder for request-reply gateway
	 * based on provided {@code route} and optional variables to expand route template.
	 * @param route the {@code route} to send requests.
	 * @param routeVariables the variables to expand route template.
	 * @return the RSocketOutboundGatewaySpec instance
	 */
	public static RSocketOutboundGatewaySpec outboundGateway(String route, Object... routeVariables) {
		return new RSocketOutboundGatewaySpec(route, routeVariables);
	}

	/**
	 * Create an {@link RSocketOutboundGatewaySpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code route} against request message.
	 * @param routeFunction the {@code Function} to evaluate {@code route} at runtime.
	 * @param <P> the expected payload type.
	 * @return the RSocketOutboundGatewaySpec instance
	 */
	public static <P> RSocketOutboundGatewaySpec outboundGateway(Function<Message<P>, ?> routeFunction) {
		return outboundGateway(new FunctionExpression<>(routeFunction));
	}

	/**
	 * Create an {@link RSocketOutboundGatewaySpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code route} against request message.
	 * @param routeExpression the SpEL {@link Expression} to evaluate {@code route} at runtime.
	 * @return the RSocketOutboundGatewaySpec instance
	 */
	public static RSocketOutboundGatewaySpec outboundGateway(Expression routeExpression) {
		return new RSocketOutboundGatewaySpec(routeExpression);
	}

	/**
	 * Create an {@link RSocketInboundGatewaySpec} builder for request-reply reactive gateway
	 * based on the provided {@code path} array for mapping.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the RSocketInboundGatewaySpec instance
	 */
	public static RSocketInboundGatewaySpec inboundGateway(String... path) {
		return new RSocketInboundGatewaySpec(path);
	}

	private RSockets() {
	}

}
