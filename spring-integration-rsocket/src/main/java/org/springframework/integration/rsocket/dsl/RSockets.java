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
