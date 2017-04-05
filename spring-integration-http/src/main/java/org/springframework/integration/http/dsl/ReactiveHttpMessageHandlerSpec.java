/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.ReactiveHttpRequestExecutingMessageHandler;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The {@link BaseHttpMessageHandlerSpec} implementation for the {@link ReactiveHttpRequestExecutingMessageHandler}.
 *
 * @author Shiliang Li
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see ReactiveHttpRequestExecutingMessageHandler
 */
public class ReactiveHttpMessageHandlerSpec
		extends BaseHttpMessageHandlerSpec<ReactiveHttpMessageHandlerSpec, ReactiveHttpRequestExecutingMessageHandler> {

	private final WebClient webClient;

	ReactiveHttpMessageHandlerSpec(URI uri, WebClient webClient) {
		this(new ValueExpression<>(uri), webClient);
	}

	ReactiveHttpMessageHandlerSpec(String uri, WebClient webClient) {
		this(new LiteralExpression(uri), webClient);
	}

	ReactiveHttpMessageHandlerSpec(Expression uriExpression, WebClient webClient) {
		super(new ReactiveHttpRequestExecutingMessageHandler(uriExpression, webClient));
		this.webClient = webClient;
	}

	@Override
	protected boolean isClientSet() {
		return this.webClient != null;
	}

}
