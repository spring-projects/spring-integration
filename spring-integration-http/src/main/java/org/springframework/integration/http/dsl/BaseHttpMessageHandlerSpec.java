/*
 * Copyright 2017-2021 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.AbstractHttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * The base {@link MessageHandlerSpec} for {@link AbstractHttpRequestExecutingMessageHandler}s.
 *
 * @param <S> the target {@link BaseHttpMessageHandlerSpec} implementation type.
 * @param <E> the target {@link AbstractHttpRequestExecutingMessageHandler} implementation type.
 *
 * @author Artem Bilan
 * @author Shiliang Li
 *
 * @since 5.0
 */
public abstract class BaseHttpMessageHandlerSpec<S extends BaseHttpMessageHandlerSpec<S, E>, E extends AbstractHttpRequestExecutingMessageHandler>
		extends MessageHandlerSpec<S, E>
		implements ComponentsRegistration {

	private final Map<String, Expression> uriVariableExpressions = new HashMap<>();

	private HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private boolean headerMapperExplicitlySet;

	public BaseHttpMessageHandlerSpec(E handler) {
		this.target = handler;
		this.target.setHeaderMapper(this.headerMapper);
	}

	protected S expectReply(boolean expectReply) {
		this.target.setExpectReply(expectReply);
		return _this();
	}

	/**
	 * Specify a {@link DefaultUriBuilderFactory.EncodingMode} for uri construction.
	 * @param encodingMode to use for uri construction.
	 * @return the spec
	 * @since 5.3
	 */
	public S encodingMode(DefaultUriBuilderFactory.EncodingMode encodingMode) {
		this.target.setEncodingMode(encodingMode);
		return _this();
	}

	/**
	 * Specify the SpEL {@link Expression} to determine {@link HttpMethod} at runtime.
	 * @param httpMethodExpression The method expression.
	 * @return the spec
	 */
	public S httpMethodExpression(Expression httpMethodExpression) {
		this.target.setHttpMethodExpression(httpMethodExpression);
		return _this();
	}

	/**
	 * Specify a {@link Function} to determine {@link HttpMethod} at runtime.
	 * @param httpMethodFunction The HTTP method {@link Function}.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> S httpMethodFunction(Function<Message<P>, ?> httpMethodFunction) {
		return httpMethodExpression(new FunctionExpression<>(httpMethodFunction));
	}

	/**
	 * Specify the {@link HttpMethod} for requests.
	 * The default method is {@code POST}.
	 * @param httpMethod the {@link HttpMethod} to use.
	 * @return the spec
	 */
	public S httpMethod(HttpMethod httpMethod) {
		this.target.setHttpMethod(httpMethod);
		return _this();
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body.
	 * Otherwise the Message instance itself is serialized.
	 * The default value is {@code true}.
	 * @param extractPayload true if the payload should be extracted.
	 * @return the spec
	 */
	public S extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return _this();
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to bytes.
	 * The default is {@code UTF-8}.
	 * @param charset The charset.
	 * @return the spec
	 */
	public S charset(String charset) {
		this.target.setCharset(charset);
		return _this();
	}

	/**
	 * Specify the expected response type for the REST request.
	 * @param expectedResponseType The expected type.
	 * @return the spec
	 */
	public S expectedResponseType(Class<?> expectedResponseType) {
		this.target.setExpectedResponseType(expectedResponseType);
		return _this();
	}

	/**
	 * Specify a {@link ParameterizedTypeReference} for the expected response type for the REST request.
	 * @param expectedResponseType The {@link ParameterizedTypeReference} for expected type.
	 * @return the spec
	 */
	public S expectedResponseType(ParameterizedTypeReference<?> expectedResponseType) {
		return expectedResponseTypeExpression(new ValueExpression<ParameterizedTypeReference<?>>(expectedResponseType));
	}

	/**
	 * Specify a SpEL {@link Expression} to determine the type for the expected response
	 * The returned value of the expression could be an instance of {@link Class} or
	 * {@link String} representing a fully qualified class name.
	 * @param expectedResponseTypeExpression The expected response type expression.
	 * @return the spec
	 */
	public S expectedResponseTypeExpression(Expression expectedResponseTypeExpression) {
		this.target.setExpectedResponseTypeExpression(expectedResponseTypeExpression);
		return _this();
	}

	/**
	 * Specify a {@link Function} to determine the type for the expected response
	 * The returned value of the expression could be an instance of {@link Class} or
	 * {@link String} representing a fully qualified class name.
	 * @param expectedResponseTypeFunction The expected response type {@link Function}.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> S expectedResponseTypeFunction(
			Function<Message<P>, ?> expectedResponseTypeFunction) {
		return expectedResponseTypeExpression(new FunctionExpression<>(expectedResponseTypeFunction));
	}


	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and {@code MessageHeaders}.
	 * @param headerMapper The header mapper.
	 * @return the spec
	 */
	public S headerMapper(HeaderMapper<HttpHeaders> headerMapper) {
		this.headerMapper = headerMapper;
		this.target.setHeaderMapper(this.headerMapper);
		this.headerMapperExplicitlySet = true;
		return _this();
	}

	/**
	 * Provide the pattern array for request headers to map.
	 * @param patterns the patterns for request headers to map.
	 * @return the spec
	 * @see DefaultHttpHeaderMapper#setOutboundHeaderNames(String[])
	 */
	public S mappedRequestHeaders(String... patterns) {
		Assert.isTrue(!this.headerMapperExplicitlySet,
				"The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': " + this.headerMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setOutboundHeaderNames(patterns);
		return _this();
	}

	/**
	 * Provide the pattern array for response headers to map.
	 * @param patterns the patterns for response headers to map.
	 * @return the current Spec.
	 * @see DefaultHttpHeaderMapper#setInboundHeaderNames(String[])
	 */
	public S mappedResponseHeaders(String... patterns) {
		Assert.isTrue(!this.headerMapperExplicitlySet,
				"The 'mappedResponseHeaders' must be specified on the provided 'headerMapper': " + this.headerMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setInboundHeaderNames(patterns);
		return _this();
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 * @param uriVariableExpressions The URI variable expressions.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public S uriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		this.uriVariableExpressions.clear();
		this.uriVariableExpressions.putAll(uriVariableExpressions);
		return _this();
	}

	/**
	 * Specify an {@link Expression} to evaluate a value for the uri template variable.
	 * @param variable the uri template variable.
	 * @param expression the expression to evaluate value for te uri template variable.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 * @see ValueExpression
	 * @see org.springframework.expression.common.LiteralExpression
	 */
	public S uriVariable(String variable, Expression expression) {
		this.uriVariableExpressions.put(variable, expression);
		return _this();
	}

	/**
	 * Specify a value SpEL expression for the uri template variable.
	 * @param variable the uri template variable.
	 * @param expression the expression to evaluate value for te uri template variable.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public S uriVariable(String variable, String expression) {
		return uriVariable(variable, PARSER.parseExpression(expression));
	}

	/**
	 * Specify a {@link Function} to evaluate a value for the uri template variable.
	 * @param variable the uri template variable.
	 * @param valueFunction the {@link Function} to evaluate a value for the uri template variable.
	 * @param <P> the payload type.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public <P> S uriVariable(String variable, Function<Message<P>, ?> valueFunction) {
		return uriVariable(variable, new FunctionExpression<>(valueFunction));
	}

	/**
	 * Specify a SpEL expression to evaluate a {@link Map} of URI variables at runtime against request message.
	 * @param uriVariablesExpression to use.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public S uriVariablesExpression(String uriVariablesExpression) {
		return uriVariablesExpression(PARSER.parseExpression(uriVariablesExpression));
	}

	/**
	 * Specify a SpEL expression to evaluate a {@link Map} of URI variables at runtime against request message.
	 * @param uriVariablesExpression to use.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public S uriVariablesExpression(Expression uriVariablesExpression) {
		this.target.setUriVariablesExpression(uriVariablesExpression);
		return _this();
	}

	/**
	 * Specify a {@link Function} to evaluate a {@link Map} of URI variables at runtime against request message.
	 * @param uriVariablesFunction the {@link Function} to use.
	 * @param <P> the payload type.
	 * @return the current Spec.
	 * @see AbstractHttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public <P> S uriVariablesFunction(Function<Message<P>, Map<String, ?>> uriVariablesFunction) {
		return uriVariablesExpression(new FunctionExpression<>(uriVariablesFunction));
	}

	/**
	 * Set to {@code true} if you wish {@code Set-Cookie} header in response to be
	 * transferred as {@code Cookie} header in subsequent interaction for a message.
	 * Defaults to false.
	 * @param transferCookies the transferCookies to set.
	 * @return the current Spec.
	 */
	public S transferCookies(boolean transferCookies) {
		this.target.setTransferCookies(transferCookies);
		return _this();
	}

	/**
	 * The flag to extract a body of the {@link org.springframework.http.ResponseEntity}
	 * for reply message payload. Defaults to true.
	 * @param extractResponseBody produce a reply message with a whole
	 * {@link org.springframework.http.ResponseEntity} or just its body.
	 * @return the current Spec.
	 * @since 5.5
	 */
	public S extractResponseBody(boolean extractResponseBody) {
		this.target.setExtractResponseBody(extractResponseBody);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		this.target.setUriVariableExpressions(this.uriVariableExpressions);
		return Collections.singletonMap(this.headerMapper, null);
	}

	protected abstract boolean isClientSet();

}
