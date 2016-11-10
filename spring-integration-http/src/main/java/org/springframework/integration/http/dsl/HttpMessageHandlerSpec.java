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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link MessageHandlerSpec} implementation for the {@link HttpRequestExecutingMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see HttpRequestExecutingMessageHandler
 */
public class HttpMessageHandlerSpec
		extends MessageHandlerSpec<HttpMessageHandlerSpec, HttpRequestExecutingMessageHandler>
		implements ComponentsRegistration {

	private final RestTemplate restTemplate;

	private final Map<String, Expression> uriVariableExpressions = new HashMap<>();

	private HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private boolean headerMapperExplicitlySet;

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

	HttpMessageHandlerSpec expectReply(boolean expectReply) {
		this.target.setExpectReply(expectReply);
		return this;
	}

	public HttpMessageHandlerSpec encodeUri(boolean encodeUri) {
		this.target.setEncodeUri(encodeUri);
		return this;
	}

	public HttpMessageHandlerSpec httpMethodExpression(Expression httpMethodExpression) {
		this.target.setHttpMethodExpression(httpMethodExpression);
		return this;
	}

	public <P> HttpMessageHandlerSpec httpMethodFunction(Function<Message<P>, ?> httpMethodFunction) {
		return httpMethodExpression(new FunctionExpression<>(httpMethodFunction));
	}

	public HttpMessageHandlerSpec httpMethod(HttpMethod httpMethod) {
		this.target.setHttpMethod(httpMethod);
		return this;
	}

	public HttpMessageHandlerSpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	public HttpMessageHandlerSpec charset(String charset) {
		this.target.setCharset(charset);
		return this;
	}

	public HttpMessageHandlerSpec expectedResponseType(Class<?> expectedResponseType) {
		this.target.setExpectedResponseType(expectedResponseType);
		return this;
	}

	public HttpMessageHandlerSpec expectedResponseType(ParameterizedTypeReference<?> expectedResponseType) {
		return expectedResponseTypeExpression(new ValueExpression<ParameterizedTypeReference<?>>(expectedResponseType));
	}

	public HttpMessageHandlerSpec expectedResponseTypeExpression(Expression expectedResponseTypeExpression) {
		this.target.setExpectedResponseTypeExpression(expectedResponseTypeExpression);
		return this;
	}

	public <P> HttpMessageHandlerSpec expectedResponseTypeFunction(
			Function<Message<P>, ?> expectedResponseTypeFunction) {
		return expectedResponseTypeExpression(new FunctionExpression<>(expectedResponseTypeFunction));
	}

	public HttpMessageHandlerSpec errorHandler(ResponseErrorHandler errorHandler) {
		Assert.isNull(this.restTemplate,
				"the 'errorHandler' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setErrorHandler(errorHandler);
		return this;
	}

	public HttpMessageHandlerSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
		Assert.isNull(this.restTemplate,
				"the 'messageConverters' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setMessageConverters(Arrays.asList(messageConverters));
		return this;
	}

	public HttpMessageHandlerSpec requestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.isNull(this.restTemplate,
				"the 'requestFactory' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setRequestFactory(requestFactory);
		return this;
	}

	public HttpMessageHandlerSpec headerMapper(HeaderMapper<HttpHeaders> headerMapper) {
		this.headerMapper = headerMapper;
		this.target.setHeaderMapper(this.headerMapper);
		this.headerMapperExplicitlySet = true;
		return this;
	}

	public HttpMessageHandlerSpec mappedRequestHeaders(String... patterns) {
		Assert.isTrue(!this.headerMapperExplicitlySet,
				"The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': " + this.headerMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setOutboundHeaderNames(patterns);
		return this;
	}

	public HttpMessageHandlerSpec mappedResponseHeaders(String... patterns) {
		Assert.isTrue(!this.headerMapperExplicitlySet,
				"The 'mappedResponseHeaders' must be specified on the provided 'headerMapper': " + this.headerMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setInboundHeaderNames(patterns);
		return this;
	}

	public HttpMessageHandlerSpec uriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		this.uriVariableExpressions.clear();
		this.uriVariableExpressions.putAll(uriVariableExpressions);
		return this;
	}

	public HttpMessageHandlerSpec uriVariable(String variable, Expression value) {
		this.uriVariableExpressions.put(variable, value);
		return this;
	}

	/**
	 * @param variable the uri template variable.
	 * @param value the expression to evaluate value for te uri template variable.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public HttpMessageHandlerSpec uriVariable(String variable, String value) {
		return uriVariable(variable, PARSER.parseExpression(value));
	}

	/**
	 * @param variable the uri template variable.
	 * @param valueFunction the function to evaluate value for te uri template variable.
	 * @param <P> the payload type.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public <P> HttpMessageHandlerSpec uriVariable(String variable, Function<Message<P>, ?> valueFunction) {
		return uriVariable(variable, new FunctionExpression<>(valueFunction));
	}

	public HttpMessageHandlerSpec uriVariablesExpression(Expression uriVariablesExpression) {
		this.target.setUriVariablesExpression(uriVariablesExpression);
		return this;
	}

	/**
	 * @param uriVariablesExpression to use.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public HttpMessageHandlerSpec uriVariablesExpression(String uriVariablesExpression) {
		return uriVariablesExpression(PARSER.parseExpression(uriVariablesExpression));
	}

	/**
	 * @param uriVariablesFunction to use.
	 * @param <P> the payload type.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public <P> HttpMessageHandlerSpec uriVariablesFunction(Function<Message<P>, Map<String, ?>> uriVariablesFunction) {
		return uriVariablesExpression(new FunctionExpression<>(uriVariablesFunction));
	}

	public HttpMessageHandlerSpec transferCookies(boolean transferCookies) {
		this.target.setTransferCookies(transferCookies);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.singletonList(this.headerMapper);
	}

}
