/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * The HTTP components Factory.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class Http {

	public static HttpMessageHandlerSpec outboundChannelAdapter(URI uri) {
		return outboundChannelAdapter(uri, null);
	}

	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri) {
		return outboundChannelAdapter(uri, null);
	}

	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction) {
		return outboundChannelAdapter(new FunctionExpression<>(uriFunction));
	}

	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression) {
		return outboundChannelAdapter(uriExpression, null);
	}

	public static HttpMessageHandlerSpec outboundChannelAdapter(URI uri, RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate).expectReply(false);
	}

	public static HttpMessageHandlerSpec outboundChannelAdapter(String uri, RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate).expectReply(false);
	}

	public static <P> HttpMessageHandlerSpec outboundChannelAdapter(Function<Message<P>, ?> uriFunction,
			RestTemplate restTemplate) {
		return outboundChannelAdapter(new FunctionExpression<>(uriFunction), restTemplate);
	}

	public static HttpMessageHandlerSpec outboundChannelAdapter(Expression uriExpression, RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uriExpression, restTemplate).expectReply(false);
	}

	public static HttpMessageHandlerSpec outboundGateway(URI uri) {
		return outboundGateway(uri, null);
	}

	public static HttpMessageHandlerSpec outboundGateway(String uri) {
		return outboundGateway(uri, null);
	}

	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction) {
		return outboundGateway(new FunctionExpression<>(uriFunction));
	}

	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression) {
		return outboundGateway(uriExpression, null);
	}

	public static HttpMessageHandlerSpec outboundGateway(URI uri, RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate);
	}

	public static HttpMessageHandlerSpec outboundGateway(String uri, RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uri, restTemplate);
	}

	public static <P> HttpMessageHandlerSpec outboundGateway(Function<Message<P>, ?> uriFunction,
			RestTemplate restTemplate) {
		return outboundGateway(new FunctionExpression<>(uriFunction), restTemplate);
	}

	public static HttpMessageHandlerSpec outboundGateway(Expression uriExpression, RestTemplate restTemplate) {
		return new HttpMessageHandlerSpec(uriExpression, restTemplate);
	}

	public static HttpControllerEndpointSpec inboundControllerAdapter(String viewName, String... path) {
		Assert.isTrue(StringUtils.hasText(viewName), "View name must not be empty");
		return inboundControllerAdapter(new LiteralExpression(viewName), path);
	}

	public static HttpControllerEndpointSpec inboundControllerAdapter(Expression viewExpression, String... path) {
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setViewExpression(viewExpression);
		return new HttpControllerEndpointSpec(controller, path);
	}

	public static HttpControllerEndpointSpec inboundControllerGateway(String viewName, String... path) {
		Assert.isTrue(StringUtils.hasText(viewName), "View name must not be empty");
		return inboundControllerGateway(new LiteralExpression(viewName), path);
	}

	public static HttpControllerEndpointSpec inboundControllerGateway(Expression viewExpression, String... path) {
		HttpRequestHandlingController controller = new HttpRequestHandlingController();
		controller.setViewExpression(viewExpression);
		return new HttpControllerEndpointSpec(controller, path);
	}

	public static HttpRequestHandlerEndpointSpec inboundChannelAdapter(String... path) {
		HttpRequestHandlingMessagingGateway httpInboundChannelAdapter = new HttpRequestHandlingMessagingGateway(false);
		return new HttpRequestHandlerEndpointSpec(httpInboundChannelAdapter, path);
	}

	public static HttpRequestHandlerEndpointSpec inboundGateway(String... path) {
		return new HttpRequestHandlerEndpointSpec(new HttpRequestHandlingMessagingGateway(), path);
	}

	private Http() {
		super();
	}

}
