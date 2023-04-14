/*
 * Copyright 2017-2023 the original author or authors.
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

package org.springframework.integration.webflux.dsl;

import java.net.URI;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The WebFlux components Factory.
 *
 * @author Artem Bilan
 * @author Shiliang Li
 *
 * @since 5.0
 */
public final class WebFlux {

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter based on provided {@link URI}.
	 * @param uri the {@link URI} to send requests.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundChannelAdapter(URI uri) {
		return outboundChannelAdapter(uri, null);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter based on provided {@code uri}.
	 * @param uri the {@code uri} to send requests.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundChannelAdapter(String uri) {
		return outboundChannelAdapter(uri, null);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter based on provided {@code Function}
	 * to evaluate target {@code uri} against request message.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param <P> the expected payload type.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static <P> WebFluxMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction) {
		return outboundChannelAdapter(new FunctionExpression<>(uriFunction));
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundChannelAdapter(Expression uriExpression) {
		return outboundChannelAdapter(uriExpression, null);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@link URI} and {@link WebClient}.
	 * @param uri the {@link URI} to send requests.
	 * @param webClient {@link WebClient} to use.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundChannelAdapter(URI uri, @Nullable WebClient webClient) {
		return new WebFluxMessageHandlerSpec(uri, webClient)
				.expectReply(false);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code uri} and {@link WebClient}.
	 * @param uri the {@code uri} to send requests.
	 * @param webClient {@link WebClient} to use.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundChannelAdapter(String uri, @Nullable WebClient webClient) {
		return new WebFluxMessageHandlerSpec(uri, webClient)
				.expectReply(false);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link WebClient} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param webClient {@link WebClient} to use.
	 * @param <P> the expected payload type.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static <P> WebFluxMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction,
			WebClient webClient) {

		return outboundChannelAdapter(new FunctionExpression<>(uriFunction), webClient);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for one-way adapter
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link WebClient} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param webClient {@link WebClient} to use.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundChannelAdapter(Expression uriExpression,
			@Nullable WebClient webClient) {

		return new WebFluxMessageHandlerSpec(uriExpression, webClient)
				.expectReply(false);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@link URI}.
	 * @param uri the {@link URI} to send requests.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundGateway(URI uri) {
		return outboundGateway(uri, null);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code uri}.
	 * @param uri the {@code uri} to send requests.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundGateway(String uri) {
		return outboundGateway(uri, null);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against request message.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param <P> the expected payload type.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static <P> WebFluxMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction) {
		return outboundGateway(new FunctionExpression<>(uriFunction));
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri} against request message.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundGateway(Expression uriExpression) {
		return outboundGateway(uriExpression, null);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@link URI} and {@link WebClient}.
	 * @param uri the {@link URI} to send requests.
	 * @param webClient {@link WebClient} to use.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundGateway(URI uri, @Nullable WebClient webClient) {
		return new WebFluxMessageHandlerSpec(uri, webClient);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code uri} and {@link WebClient}.
	 * @param uri the {@code uri} to send requests.
	 * @param webClient {@link WebClient} to use.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundGateway(String uri, @Nullable WebClient webClient) {
		return new WebFluxMessageHandlerSpec(uri, webClient);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link WebClient} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param webClient {@link WebClient} to use.
	 * @param <P> the expected payload type.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static <P> WebFluxMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction,
			@Nullable WebClient webClient) {

		return outboundGateway(new FunctionExpression<>(uriFunction), webClient);
	}

	/**
	 * Create an {@link WebFluxMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link WebClient} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param webClient {@link WebClient} to use.
	 * @return the WebFluxMessageHandlerSpec instance
	 */
	public static WebFluxMessageHandlerSpec outboundGateway(Expression uriExpression,
			@Nullable WebClient webClient) {

		return new WebFluxMessageHandlerSpec(uriExpression, webClient);
	}

	/**
	 * Create an {@link WebFluxInboundEndpointSpec} builder for one-way reactive adapter
	 * based on the provided {@code path} array for mapping.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the WebFluxInboundEndpointSpec instance
	 */
	public static WebFluxInboundEndpointSpec inboundChannelAdapter(String... path) {
		WebFluxInboundEndpoint httpInboundChannelAdapter = new WebFluxInboundEndpoint(false);
		return new WebFluxInboundEndpointSpec(httpInboundChannelAdapter, path);
	}

	/**
	 * Create an {@link WebFluxInboundEndpointSpec} builder for request-reply reactive gateway
	 * based on the provided {@code path} array for mapping.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the WebFluxInboundEndpointSpec instance
	 */
	public static WebFluxInboundEndpointSpec inboundGateway(String... path) {
		return new WebFluxInboundEndpointSpec(new WebFluxInboundEndpoint(), path);
	}

	private WebFlux() {
	}

}
