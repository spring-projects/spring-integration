/*
 * Copyright 2016-2017 the original author or authors.
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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link BaseHttpMessageHandlerSpec} implementation for the {@link HttpRequestExecutingMessageHandler}.
 *
 * @author Artem Bilan
 * @author Shiliang Li
 *
 * @since 5.0
 *
 * @see HttpRequestExecutingMessageHandler
 */
public class HttpMessageHandlerSpec
		extends BaseHttpMessageHandlerSpec<HttpMessageHandlerSpec, HttpRequestExecutingMessageHandler> {

	private final RestTemplate restTemplate;

	HttpMessageHandlerSpec(URI uri, RestTemplate restTemplate) {
		this(new ValueExpression<>(uri), restTemplate);
	}

	HttpMessageHandlerSpec(String uri, RestTemplate restTemplate) {
		this(new LiteralExpression(uri), restTemplate);
	}

	HttpMessageHandlerSpec(Expression uriExpression, RestTemplate restTemplate) {
		this.target = new HttpRequestExecutingMessageHandler(uriExpression, restTemplate);
		this.target.setUriVariableExpressions(this.uriVariableExpressions);
		this.target.setHeaderMapper(this.headerMapper);
		this.restTemplate = restTemplate;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @param requestFactory The request factory.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec requestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.isNull(this.restTemplate,
				"the 'requestFactory' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setRequestFactory(requestFactory);
		return this;
	}

	@Override
	protected boolean isRestTemplateSet() {
		return this.restTemplate != null;
	}
}
