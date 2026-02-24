/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.http.dsl;

import java.net.URI;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * The HTTP components Factory.
 *
 * @author Artem Bilan
 * @author Shiliang Li
 * @author Arun Sethumadhavan
 *
 * @since 5.0
 */
public final class Http {

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided {@link URI}.
	 * @param uri the {@link URI} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(URI uri) {
		return outboundChannelAdapter(uri, (RestClient) null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided {@code uri}.
	 * @param uri the {@code uri} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri) {
		return outboundChannelAdapter(uri, (RestClient) null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided {@code Function}
	 * to evaluate target {@code uri} against a request message.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction) {
		return outboundChannelAdapter(new FunctionExpression<>(uriFunction));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided SpEL {@link Expression}
	 * to evaluate target {@code uri} against a request message.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression) {
		return outboundChannelAdapter(uriExpression, (RestClient) null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@link URI} and {@link RestTemplate}.
	 * @param uri the {@link URI} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static HttpMessageHandlerSpec outboundChannelAdapter(URI uri, @Nullable RestTemplate restTemplate) {
		return outboundChannelAdapter(uri, toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@link URI} and {@link RestClient}.
	 * @param uri the {@link URI} to send requests.
	 * @param restClient {@link RestClient} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(URI uri, @Nullable RestClient restClient) {
		return outboundGatewaySpec(uri, restClient).expectReply(false);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code uri} and {@link RestTemplate}.
	 * @param uri the {@code uri} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri, @Nullable RestTemplate restTemplate) {
		return outboundChannelAdapter(uri, toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code uri} and {@link RestClient}.
	 * @param uri the {@code uri} to send requests.
	 * @param restClient {@link RestClient} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri, @Nullable RestClient restClient) {
		return outboundGatewaySpec(uri, restClient).expectReply(false);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link RestTemplate} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction,
			RestTemplate restTemplate) {

		return outboundChannelAdapter(new FunctionExpression<>(uriFunction), toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link RestClient} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param restClient {@link RestClient} to use.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction,
			@Nullable RestClient restClient) {

		return outboundChannelAdapter(new FunctionExpression<>(uriFunction), restClient);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link RestTemplate} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression,
			@Nullable RestTemplate restTemplate) {

		return outboundChannelAdapter(uriExpression, toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link RestClient} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param restClient {@link RestClient} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression,
			@Nullable RestClient restClient) {

		return outboundGatewaySpec(uriExpression, restClient).expectReply(false);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway based on provided {@link URI}.
	 * @param uri the {@link URI} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(URI uri) {
		return outboundGateway(uri, (RestClient) null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway based on provided {@code uri}.
	 * @param uri the {@code uri} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(String uri) {
		return outboundGateway(uri, (RestClient) null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against a request message.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction) {
		return outboundGateway(new FunctionExpression<>(uriFunction));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri} against a request message.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression) {
		return outboundGateway(uriExpression, (RestClient) null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@link URI} and {@link RestTemplate}.
	 * @param uri the {@link URI} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static HttpMessageHandlerSpec outboundGateway(URI uri, @Nullable RestTemplate restTemplate) {
		return outboundGateway(uri, toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@link URI} and {@link RestClient}.
	 * @param uri the {@link URI} to send requests.
	 * @param restClient {@link RestClient} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static HttpMessageHandlerSpec outboundGateway(URI uri, @Nullable RestClient restClient) {
		return outboundGatewaySpec(uri, restClient);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code uri} and {@link RestTemplate}.
	 * @param uri the {@code uri} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static HttpMessageHandlerSpec outboundGateway(String uri, @Nullable RestTemplate restTemplate) {
		return outboundGateway(uri, toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code uri} and {@link RestClient}.
	 * @param uri the {@code uri} to send requests.
	 * @param restClient {@link RestClient} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static HttpMessageHandlerSpec outboundGateway(String uri, @Nullable RestClient restClient) {
		return outboundGatewaySpec(uri, restClient);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link RestTemplate} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction,
			RestTemplate restTemplate) {

		return outboundGateway(new FunctionExpression<>(uriFunction), toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link RestClient} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param restClient {@link RestClient} to use.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction,
			@Nullable RestClient restClient) {

		return outboundGateway(new FunctionExpression<>(uriFunction), restClient);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link RestTemplate} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @deprecated Since 7.1 in favor of {@link RestClient}-based configuration.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression,
			@Nullable RestTemplate restTemplate) {

		return outboundGateway(uriExpression, toRestClient(restTemplate));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link RestClient} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param restClient {@link RestClient} to use.
	 * @return the HttpMessageHandlerSpec instance
	 * @since 7.1
	 */
	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression, @Nullable RestClient restClient) {
		return outboundGatewaySpec(uriExpression, restClient);
	}

	private static HttpMessageHandlerSpec outboundGatewaySpec(URI uri, @Nullable RestClient restClient) {
		return new HttpMessageHandlerSpec(uri, restClient);
	}

	private static HttpMessageHandlerSpec outboundGatewaySpec(String uri, @Nullable RestClient restClient) {
		return new HttpMessageHandlerSpec(uri, restClient);
	}

	private static HttpMessageHandlerSpec outboundGatewaySpec(Expression uriExpression,
			@Nullable RestClient restClient) {

		return new HttpMessageHandlerSpec(uriExpression, restClient);
	}

	private static @Nullable RestClient toRestClient(@Nullable RestTemplate restTemplate) {
		return restTemplate != null ? RestClient.create(restTemplate) : null;
	}

	/**
	 * Create an {@link HttpControllerEndpointSpec} builder for one-way adapter
	 * based on the provided MVC {@code viewName} and {@code path} array for mapping.
	 * @param viewName the MVC view name to build at the end of the request.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the HttpControllerEndpointSpec instance
	 */
	public static HttpControllerEndpointSpec inboundControllerAdapter(String viewName, String... path) {
		Assert.hasText(viewName, "View name must not be empty");
		return inboundControllerAdapter(new LiteralExpression(viewName), path);
	}

	/**
	 * Create an {@link HttpControllerEndpointSpec} builder for one-way adapter
	 * based on the provided SpEL expression  and {@code path} array for mapping.
	 * @param viewExpression the SpEL expression to evaluate MVC view name to build at the end of the request.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the HttpControllerEndpointSpec instance
	 */
	public static HttpControllerEndpointSpec inboundControllerAdapter(Expression viewExpression, String... path) {
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setViewExpression(viewExpression);
		return new HttpControllerEndpointSpec(controller, path);
	}

	/**
	 * Create an {@link HttpControllerEndpointSpec} builder for request-reply gateway
	 * based on the provided MVC {@code viewName} and {@code path} array for mapping.
	 * @param viewName the MVC view name to build at the end of the request.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the HttpControllerEndpointSpec instance
	 */
	public static HttpControllerEndpointSpec inboundControllerGateway(String viewName, String... path) {
		Assert.hasText(viewName, "View name must not be empty");
		return inboundControllerGateway(new LiteralExpression(viewName), path);
	}

	/**
	 * Create an {@link HttpControllerEndpointSpec} builder for request-reply gateway
	 * based on the provided SpEL expression  and {@code path} array for mapping.
	 * @param viewExpression the SpEL expression to evaluate MVC view name to build at the end of the request.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the HttpControllerEndpointSpec instance
	 */
	public static HttpControllerEndpointSpec inboundControllerGateway(Expression viewExpression, String... path) {
		HttpRequestHandlingController controller = new HttpRequestHandlingController();
		controller.setViewExpression(viewExpression);
		return new HttpControllerEndpointSpec(controller, path);
	}

	/**
	 * Create an {@link HttpRequestHandlerEndpointSpec} builder for one-way adapter
	 * based on the provided {@code path} array for mapping.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the HttpRequestHandlerEndpointSpec instance
	 */
	public static HttpRequestHandlerEndpointSpec inboundChannelAdapter(String... path) {
		HttpRequestHandlingMessagingGateway httpInboundChannelAdapter = new HttpRequestHandlingMessagingGateway(false);
		return new HttpRequestHandlerEndpointSpec(httpInboundChannelAdapter, path);
	}

	/**
	 * Create an {@link HttpRequestHandlerEndpointSpec} builder for request-reply gateway
	 * based on the provided {@code path} array for mapping.
	 * @param path the path mapping URIs (e.g. "/myPath.do").
	 * @return the HttpRequestHandlerEndpointSpec instance
	 */
	public static HttpRequestHandlerEndpointSpec inboundGateway(String... path) {
		return new HttpRequestHandlerEndpointSpec(new HttpRequestHandlingMessagingGateway(), path);
	}

	private Http() {
	}

}
