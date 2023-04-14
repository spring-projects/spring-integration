/*
 * Copyright 2016-2023 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * The HTTP components Factory.
 *
 * @author Artem Bilan
 * @author Shiliang Li
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
		return outboundChannelAdapter(uri, null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided {@code uri}.
	 * @param uri the {@code uri} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri) {
		return outboundChannelAdapter(uri, null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided {@code Function}
	 * to evaluate target {@code uri} against request message.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction) {
		return outboundChannelAdapter(new FunctionExpression<>(uriFunction));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter based on provided SpEL {@link Expression}
	 * to evaluate target {@code uri} against request message.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression) {
		return outboundChannelAdapter(uriExpression, null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@link URI} and {@link RestTemplate}.
	 * @param uri the {@link URI} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(URI uri, @Nullable RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate).expectReply(false);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code uri} and {@link RestTemplate}.
	 * @param uri the {@code uri} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri, @Nullable RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate).expectReply(false);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link RestTemplate} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction,
			RestTemplate restTemplate) {

		return outboundChannelAdapter(new FunctionExpression<>(uriFunction), restTemplate);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for one-way adapter
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link RestTemplate} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression,
			@Nullable RestTemplate restTemplate) {

		return new HttpMessageHandlerSpec(uriExpression, restTemplate).expectReply(false);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway based on provided {@link URI}.
	 * @param uri the {@link URI} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(URI uri) {
		return outboundGateway(uri, null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway based on provided {@code uri}.
	 * @param uri the {@code uri} to send requests.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(String uri) {
		return outboundGateway(uri, null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against request message.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction) {
		return outboundGateway(new FunctionExpression<>(uriFunction));
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri} against request message.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression) {
		return outboundGateway(uriExpression, null);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@link URI} and {@link RestTemplate}.
	 * @param uri the {@link URI} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(URI uri, @Nullable RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code uri} and {@link RestTemplate}.
	 * @param uri the {@code uri} to send requests.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(String uri, @Nullable RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided {@code Function} to evaluate target {@code uri} against request message
	 * and {@link RestTemplate} for HTTP exchanges.
	 * @param uriFunction the {@code Function} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @param <P> the expected payload type.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction,
			RestTemplate restTemplate) {

		return outboundGateway(new FunctionExpression<>(uriFunction), restTemplate);
	}

	/**
	 * Create an {@link HttpMessageHandlerSpec} builder for request-reply gateway
	 * based on provided SpEL {@link Expression} to evaluate target {@code uri}
	 * against request message and {@link RestTemplate} for HTTP exchanges.
	 * @param uriExpression the SpEL {@link Expression} to evaluate {@code uri} at runtime.
	 * @param restTemplate {@link RestTemplate} to use.
	 * @return the HttpMessageHandlerSpec instance
	 */
	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression,
			@Nullable RestTemplate restTemplate) {

		return new HttpMessageHandlerSpec(uriExpression, restTemplate);
	}

	/**
	 * Create an {@link HttpControllerEndpointSpec} builder for one-way adapter
	 * based on the provided MVC {@code viewName} and {@code path} array for mapping.
	 * @param viewName the MVC view name to build in the end of request.
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
	 * @param viewExpression the SpEL expression to evaluate MVC view name to build in the end of request.
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
	 * @param viewName the MVC view name to build in the end of request.
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
	 * @param viewExpression the SpEL expression to evaluate MVC view name to build in the end of request.
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
