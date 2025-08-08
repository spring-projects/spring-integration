/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.webflux.inbound;

import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link HandlerResultHandler} implementation to handle the result of the
 * {@link WebFluxInboundEndpoint} execution. Actually just return the
 * {@code result.getReturnValue()} which essentially is expected {@code Mono<Void>}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see WebFluxInboundEndpoint
 */
public class IntegrationHandlerResultHandler implements HandlerResultHandler, Ordered {

	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		return handler instanceof HandlerMethod
				&& WebFluxInboundEndpoint.class.isAssignableFrom(((HandlerMethod) handler).getBeanType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Object returnValue = result.getReturnValue();
		return returnValue == null ? Mono.empty() : (Mono<Void>) returnValue;
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}

}
