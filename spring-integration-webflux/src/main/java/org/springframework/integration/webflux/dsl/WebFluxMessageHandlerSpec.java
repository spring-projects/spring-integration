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

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.dsl.BaseHttpMessageHandlerSpec;
import org.springframework.integration.webflux.outbound.WebFluxRequestExecutingMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The {@link BaseHttpMessageHandlerSpec} implementation for the {@link WebFluxRequestExecutingMessageHandler}.
 *
 * @author Shiliang Li
 * @author Artem Bilan
 * @author Abhijit Sarkar
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @see WebFluxRequestExecutingMessageHandler
 */
public class WebFluxMessageHandlerSpec
		extends BaseHttpMessageHandlerSpec<WebFluxMessageHandlerSpec, WebFluxRequestExecutingMessageHandler> {

	@Nullable
	protected final WebClient webClient; // NOSONAR - final

	protected WebFluxMessageHandlerSpec(URI uri, @Nullable WebClient webClient) {
		this(new ValueExpression<>(uri), webClient);
	}

	protected WebFluxMessageHandlerSpec(String uri, @Nullable WebClient webClient) {
		this(new LiteralExpression(uri), webClient);
	}

	protected WebFluxMessageHandlerSpec(Expression uriExpression, @Nullable WebClient webClient) {
		super(new WebFluxRequestExecutingMessageHandler(uriExpression, webClient));
		this.webClient = webClient;
	}

	/**
	 * The boolean flag to identify if the reply payload should be as a
	 * {@link reactor.core.publisher.Flux} from the response body
	 * or as resolved value from the {@link reactor.core.publisher.Mono}
	 * of the response body.
	 * Defaults to {@code false} - simple value is pushed downstream.
	 * Makes sense when {@code expectedResponseType} is configured.
	 * @param replyPayloadToFlux represent reply payload as a
	 * {@link reactor.core.publisher.Flux} or as a value from the
	 * {@link reactor.core.publisher.Mono}.
	 * @return the spec
	 * @since 5.0.1
	 * @see WebFluxRequestExecutingMessageHandler#setReplyPayloadToFlux(boolean)
	 */
	public WebFluxMessageHandlerSpec replyPayloadToFlux(boolean replyPayloadToFlux) {
		this.target.setReplyPayloadToFlux(replyPayloadToFlux);
		return this;
	}

	/**
	 * Specify a {@link BodyExtractor} as an alternative to the {@code expectedResponseType}
	 * to allow to get low-level access to the received {@link ClientHttpResponse}.
	 * @param bodyExtractor the {@link BodyExtractor} to use.
	 * @return the spec
	 * @since 5.0.1
	 * @see WebFluxRequestExecutingMessageHandler#setBodyExtractor(BodyExtractor)
	 */
	public WebFluxMessageHandlerSpec bodyExtractor(BodyExtractor<?, ? super ClientHttpResponse> bodyExtractor) {
		this.target.setBodyExtractor(bodyExtractor);
		return this;
	}

	/**
	 * Configure a type for a request {@link org.reactivestreams.Publisher} elements.
	 * @param publisherElementType  the type of the request {@link org.reactivestreams.Publisher} elements.
	 * @return the spec
	 * @since 5.2
	 * @see WebFluxRequestExecutingMessageHandler#setPublisherElementType
	 */
	public WebFluxMessageHandlerSpec publisherElementType(Class<?> publisherElementType) {
		this.target.setPublisherElementType(publisherElementType);
		return this;
	}

	/**
	 * Configure a {@link ParameterizedTypeReference} for a request {@link org.reactivestreams.Publisher} elements.
	 * @param publisherElementType the type of the request {@link org.reactivestreams.Publisher} elements.
	 * @return the spec
	 * @since 5.2
	 * @see WebFluxRequestExecutingMessageHandler#setPublisherElementType
	 */
	public WebFluxMessageHandlerSpec publisherElementType(ParameterizedTypeReference<?> publisherElementType) {
		return publisherElementTypeExpression(new ValueExpression<>(publisherElementType));
	}

	/**
	 * Configure a {@link Function} to evaluate a request {@link org.reactivestreams.Publisher}
	 * elements type at runtime against a request message.
	 * @param typeFunction the {@link Function} to evaluate a type for the request
	 * {@link org.reactivestreams.Publisher} elements.
	 * @param <P> the expected payload type.
	 * @return the spec
	 * @since 5.2
	 * @see WebFluxRequestExecutingMessageHandler#setPublisherElementTypeExpression(Expression)
	 */
	public <P> WebFluxMessageHandlerSpec publisherElementTypeFunction(Function<Message<P>, ?> typeFunction) {
		return publisherElementTypeExpression(new FunctionExpression<>(typeFunction));
	}

	/**
	 * Configure a SpEL expression to evaluate a request {@link org.reactivestreams.Publisher}
	 * elements type at runtime against a request message.
	 * @param publisherElementTypeExpression the expression to evaluate a type for the request
	 * {@link org.reactivestreams.Publisher} elements.
	 * @return the spec
	 * @since 5.2
	 * @see WebFluxRequestExecutingMessageHandler#setPublisherElementTypeExpression(Expression)
	 */
	public WebFluxMessageHandlerSpec publisherElementTypeExpression(Expression publisherElementTypeExpression) {
		this.target.setPublisherElementTypeExpression(publisherElementTypeExpression);
		return this;
	}

	@Override
	protected boolean isClientSet() {
		return this.webClient != null;
	}

	@Override
	protected WebFluxMessageHandlerSpec expectReply(boolean expectReply) { // NOSONAR increases visibility
		return super.expectReply(expectReply);
	}

}
