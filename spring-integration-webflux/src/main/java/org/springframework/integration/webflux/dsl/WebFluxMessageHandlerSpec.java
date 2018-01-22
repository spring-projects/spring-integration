/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.webflux.dsl;

import java.net.URI;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.dsl.BaseHttpMessageHandlerSpec;
import org.springframework.integration.webflux.outbound.WebFluxRequestExecutingMessageHandler;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The {@link BaseHttpMessageHandlerSpec} implementation for the {@link WebFluxRequestExecutingMessageHandler}.
 *
 * @author Shiliang Li
 * @author Artem Bilan
 * @author Abhijit Sarkar
 *
 * @since 5.0
 *
 * @see WebFluxRequestExecutingMessageHandler
 */
public class WebFluxMessageHandlerSpec
		extends BaseHttpMessageHandlerSpec<WebFluxMessageHandlerSpec, WebFluxRequestExecutingMessageHandler> {

	private final WebClient webClient;

	WebFluxMessageHandlerSpec(URI uri, WebClient webClient) {
		this(new ValueExpression<>(uri), webClient);
	}

	WebFluxMessageHandlerSpec(String uri, WebClient webClient) {
		this(new LiteralExpression(uri), webClient);
	}

	WebFluxMessageHandlerSpec(Expression uriExpression, WebClient webClient) {
		super(new WebFluxRequestExecutingMessageHandler(uriExpression, webClient));
		this.webClient = webClient;
	}

	/**
	 * The boolean flag to identify if the reply payload should be as a {@link Flux} from the response body
	 * or as resolved value from the {@link Mono} of the response body.
	 * Defaults to {@code false} - simple value is pushed downstream.
	 * Makes sense when {@code expectedResponseType} is configured.
	 * @param replyPayloadToFlux represent reply payload as a {@link Flux} or as a value from the {@link Mono}.
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
	public WebFluxMessageHandlerSpec bodyExtractor(BodyExtractor<?, ClientHttpResponse> bodyExtractor) {
		this.target.setBodyExtractor(bodyExtractor);
		return this;
	}

	@Override
	protected boolean isClientSet() {
		return this.webClient != null;
	}

	@Override
	protected WebFluxMessageHandlerSpec expectReply(boolean expectReply) {
		return super.expectReply(expectReply);
	}

}
