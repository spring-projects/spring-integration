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
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.AsyncHttpRequestExecutingMessageHandler;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;

/**
 * The {@link BaseHttpMessageHandlerSpec} implementation for the {@link AsyncHttpRequestExecutingMessageHandler}.
 * @author Shiliang Li
 * @since 5.0
 * @see AsyncHttpRequestExecutingMessageHandler
 */
public class AsyncHttpMessageHandlerSpec
		extends BaseHttpMessageHandlerSpec<AsyncHttpMessageHandlerSpec, AsyncHttpRequestExecutingMessageHandler> {

	private final AsyncRestTemplate asyncRestTemplate;

	AsyncHttpMessageHandlerSpec(URI uri, AsyncRestTemplate asyncRestTemplate) {
		this(new ValueExpression<>(uri), asyncRestTemplate);
	}

	AsyncHttpMessageHandlerSpec(String uri, AsyncRestTemplate asyncRestTemplate) {
		this(new LiteralExpression(uri), asyncRestTemplate);
	}

	AsyncHttpMessageHandlerSpec(Expression uriExpression, AsyncRestTemplate asyncRestTemplate) {
		this.target = new AsyncHttpRequestExecutingMessageHandler(uriExpression, asyncRestTemplate);
		this.target.setUriVariableExpressions(this.uriVariableExpressions);
		this.target.setHeaderMapper(this.headerMapper);
		this.asyncRestTemplate = asyncRestTemplate;
	}

	/**
	 * Set the {@link AsyncClientHttpRequestFactory} for the underlying {@link AsyncRestTemplate}.
	 * @param asyncRequestFactory The request factory.
	 * @return the spec
	 */
	public AsyncHttpMessageHandlerSpec asyncRequestFactory(AsyncClientHttpRequestFactory asyncRequestFactory) {
		Assert.isNull(this.asyncRestTemplate,
				"the 'requestFactory' must be specified on the provided 'restTemplate': " + this.asyncRestTemplate);
		this.target.setAsyncRequestFactory(asyncRequestFactory);
		return this;
	}

	@Override
	protected boolean isRestTemplateSet() {
		return this.asyncRestTemplate != null;
	}
}
