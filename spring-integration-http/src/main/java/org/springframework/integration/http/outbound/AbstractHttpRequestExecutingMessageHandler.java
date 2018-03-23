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

package org.springframework.integration.http.outbound;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.xml.transform.Source;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Base class for http outbound adapter/gateway.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Wallace Wadge
 * @author Shiliang Li
 *
 * @since 5.0
 */
public abstract class AbstractHttpRequestExecutingMessageHandler extends AbstractReplyProducingMessageHandler {

	private static final List<HttpMethod> noBodyHttpMethods =
			Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE);

	private final Map<String, Expression> uriVariableExpressions = new HashMap<>();

	private final Expression uriExpression;

	private StandardEvaluationContext evaluationContext;

	private SimpleEvaluationContext simpleEvaluationContext;

	private boolean trustedSpel;

	private volatile boolean encodeUri = true;

	private volatile Expression httpMethodExpression = new ValueExpression<>(HttpMethod.POST);

	private volatile boolean expectReply = true;

	private volatile Expression expectedResponseTypeExpression;

	private volatile boolean extractPayload = true;

	private volatile boolean extractPayloadExplicitlySet = false;

	private volatile Charset charset = Charset.forName("UTF-8");

	private volatile boolean transferCookies = false;

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private volatile Expression uriVariablesExpression;

	public AbstractHttpRequestExecutingMessageHandler(Expression uriExpression) {
		Assert.notNull(uriExpression, "URI Expression is required");
		this.uriExpression = uriExpression;
	}

	/**
	 * Specify whether the real URI should be encoded after <code>uriVariables</code>
	 * expanding and before send request via {@link RestTemplate}. The default value is <code>true</code>.
	 *
	 * @param encodeUri true if the URI should be encoded.
	 *
	 * @see UriComponentsBuilder
	 */
	public void setEncodeUri(boolean encodeUri) {
		this.encodeUri = encodeUri;
	}

	/**
	 * Specify the SpEL {@link Expression} to determine {@link HttpMethod} at runtime.
	 * @param httpMethodExpression The method expression.
	 */
	public void setHttpMethodExpression(Expression httpMethodExpression) {
		Assert.notNull(httpMethodExpression, "'httpMethodExpression' must not be null");
		this.httpMethodExpression = httpMethodExpression;
	}

	/**
	 * Specify the {@link HttpMethod} for requests.
	 * The default method is {@code POST}.
	 * @param httpMethod The method.
	 */
	public void setHttpMethod(HttpMethod httpMethod) {
		Assert.notNull(httpMethod, "'httpMethod' must not be null");
		this.httpMethodExpression = new ValueExpression<HttpMethod>(httpMethod);
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body.
	 * Otherwise the Message instance itself is serialized.
	 * The default value is {@code true}.
	 * @param extractPayload true if the payload should be extracted.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
		this.extractPayloadExplicitlySet = true;
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to bytes.
	 * The default is {@code UTF-8}.
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = Charset.forName(charset);
	}

	/**
	 * @return whether a reply Message is expected.
	 * @see AbstractHttpRequestExecutingMessageHandler#setExpectReply(boolean)
	 */
	public boolean isExpectReply() {
		return this.expectReply;
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 * @param expectReply true if a reply is expected.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Specify the expected response type for the REST request
	 * otherwise the default response type is {@link ResponseEntity} and will
	 * be returned as a payload of the reply Message.
	 * To take advantage of the HttpMessageConverters
	 * registered on this adapter, provide a different type).
	 *
	 * @param expectedResponseType The expected type.
	 *
	 * Also see {@link #setExpectedResponseTypeExpression(Expression)}
	 */
	public void setExpectedResponseType(Class<?> expectedResponseType) {
		Assert.notNull(expectedResponseType, "'expectedResponseType' must not be null");
		this.expectedResponseTypeExpression = new ValueExpression<Class<?>>(expectedResponseType);
	}

	/**
	 * Specify the {@link Expression} to determine the type for the expected response
	 * The returned value of the expression could be an instance of {@link Class} or
	 * {@link String} representing a fully qualified class name.
	 * @param expectedResponseTypeExpression The expected response type expression.
	 * Also see {@link #setExpectedResponseType}
	 */
	public void setExpectedResponseTypeExpression(Expression expectedResponseTypeExpression) {
		this.expectedResponseTypeExpression = expectedResponseTypeExpression;
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 * @param headerMapper The header mapper.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 *
	 * @param uriVariableExpressions The URI variable expressions.
	 */
	public void setUriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		synchronized (this.uriVariableExpressions) {
			this.uriVariableExpressions.clear();
			this.uriVariableExpressions.putAll(uriVariableExpressions);
		}
	}

	/**
	 * Set the {@link Expression} to evaluate against the outbound message; the expression
	 * must evaluate to a Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 *
	 * @param uriVariablesExpression The URI variables expression.
	 */
	public void setUriVariablesExpression(Expression uriVariablesExpression) {
		this.uriVariablesExpression = uriVariablesExpression;
	}

	/**
	 * Set to true if you wish 'Set-Cookie' headers in responses to be
	 * transferred as 'Cookie' headers in subsequent interactions for
	 * a message.
	 *
	 * @param transferCookies the transferCookies to set.
	 */
	public void setTransferCookies(boolean transferCookies) {
		this.transferCookies = transferCookies;
	}

	/**
	 * Set to true if you trust the source of SpEL expressions used to evaluate URI
	 * variables. Default is false, which means a {@link SimpleEvaluationContext} is used
	 * for evaluating such expressions, which restricts the use of some SpEL capabilities.
	 * @param trustedSpel true to trust.
	 * @since 4.3.15.
	 */
	public void setTrustedSpel(boolean trustedSpel) {
		this.trustedSpel = trustedSpel;
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		this.simpleEvaluationContext = ExpressionUtils.createSimpleEvaluationContext(this.getBeanFactory());
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		HttpMethod httpMethod = determineHttpMethod(requestMessage);

		if (!shouldIncludeRequestBody(httpMethod) && this.extractPayloadExplicitlySet) {
			if (logger.isWarnEnabled()) {
				logger.warn("The 'extractPayload' attribute has no relevance for the current request " +
						"since the HTTP Method is '" + httpMethod +
						"', and no request body will be sent for that method.");
			}
		}

		Object expectedResponseType = determineExpectedResponseType(requestMessage);

		HttpEntity<?> httpRequest = generateHttpRequest(requestMessage, httpMethod);
		return exchange(() -> generateUri(requestMessage), httpMethod, httpRequest, expectedResponseType,
				requestMessage);
	}

	protected abstract Object exchange(Supplier<URI> uriSupplier, HttpMethod httpMethod, HttpEntity<?> httpRequest,
			Object expectedResponseType, Message<?> requestMessage);

	private URI generateUri(Message<?> requestMessage) {
		Object uri = this.uriExpression.getValue(this.evaluationContext, requestMessage);
		Assert.state(uri instanceof String || uri instanceof URI,
				"'uriExpression' evaluation must result in a 'String' or 'URI' instance, not: "
						+ (uri == null ? "null" : uri.getClass()));
		Map<String, ?> uriVariables = determineUriVariables(requestMessage);
		UriComponentsBuilder uriComponentsBuilder = uri instanceof String
				? UriComponentsBuilder.fromUriString((String) uri)
				: UriComponentsBuilder.fromUri((URI) uri);
		UriComponents uriComponents = uriComponentsBuilder.buildAndExpand(uriVariables);
		try {
			return this.encodeUri ? uriComponents.toUri() : new URI(uriComponents.toUriString());
		}
		catch (URISyntaxException e) {
			throw new MessageHandlingException(requestMessage, "Invalid URI [" + uri + "]", e);
		}
	}

	protected Object getReply(ResponseEntity<?> httpResponse) {
		if (this.expectReply) {
			HttpHeaders httpHeaders = httpResponse.getHeaders();
			Map<String, Object> headers = this.headerMapper.toHeaders(httpHeaders);
			if (this.transferCookies) {
				this.doConvertSetCookie(headers);
			}

			AbstractIntegrationMessageBuilder<?> replyBuilder = null;
			MessageBuilderFactory messageBuilderFactory = getMessageBuilderFactory();
			if (httpResponse.hasBody()) {
				Object responseBody = httpResponse.getBody();
				replyBuilder = (responseBody instanceof Message<?>)
						? messageBuilderFactory.fromMessage((Message<?>) responseBody)
						: messageBuilderFactory.withPayload(responseBody);

			}
			else {
				replyBuilder = messageBuilderFactory.withPayload(httpResponse);
			}
			replyBuilder.setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE,
					httpResponse.getStatusCode());
			return replyBuilder.copyHeaders(headers);
		}
		return null;
	}

	/**
	 * Convert Set-Cookie to Cookie
	 */
	private void doConvertSetCookie(Map<String, Object> headers) {
		String keyName = null;
		for (String key : headers.keySet()) {
			if (key.equalsIgnoreCase(DefaultHttpHeaderMapper.SET_COOKIE)) {
				keyName = key;
				break;
			}
		}
		if (keyName != null) {
			Object cookies = headers.remove(keyName);
			headers.put(DefaultHttpHeaderMapper.COOKIE, cookies);
			if (logger.isDebugEnabled()) {
				logger.debug("Converted Set-Cookie header to Cookie for: "
						+ cookies);
			}
		}
	}

	private HttpEntity<?> generateHttpRequest(Message<?> message, HttpMethod httpMethod) {
		Assert.notNull(message, "message must not be null");
		return (this.extractPayload) ? this.createHttpEntityFromPayload(message, httpMethod)
				: this.createHttpEntityFromMessage(message, httpMethod);
	}

	private HttpEntity<?> createHttpEntityFromPayload(Message<?> message, HttpMethod httpMethod) {
		Object payload = message.getPayload();
		if (payload instanceof HttpEntity<?>) {
			// payload is already an HttpEntity, just return it as-is
			return (HttpEntity<?>) payload;
		}
		HttpHeaders httpHeaders = this.mapHeaders(message);
		if (!shouldIncludeRequestBody(httpMethod)) {
			return new HttpEntity<>(httpHeaders);
		}
		// otherwise, we are creating a request with a body and need to deal with the content-type header as well
		if (httpHeaders.getContentType() == null) {
			MediaType contentType = (payload instanceof String)
					? resolveContentType((String) payload, this.charset)
					: resolveContentType(payload);
			httpHeaders.setContentType(contentType);
		}
		if (MediaType.APPLICATION_FORM_URLENCODED.equals(httpHeaders.getContentType()) ||
				MediaType.MULTIPART_FORM_DATA.equals(httpHeaders.getContentType())) {
			if (!(payload instanceof MultiValueMap)) {
				payload = this.convertToMultiValueMap((Map<?, ?>) payload);
			}
		}
		return new HttpEntity<>(payload, httpHeaders);
	}

	private HttpEntity<?> createHttpEntityFromMessage(Message<?> message, HttpMethod httpMethod) {
		HttpHeaders httpHeaders = mapHeaders(message);
		if (shouldIncludeRequestBody(httpMethod)) {
			httpHeaders.setContentType(new MediaType("application", "x-java-serialized-object"));
			return new HttpEntity<Object>(message, httpHeaders);
		}
		return new HttpEntity<>(httpHeaders);
	}

	protected HttpHeaders mapHeaders(Message<?> message) {
		HttpHeaders httpHeaders = new HttpHeaders();
		this.headerMapper.fromHeaders(message.getHeaders(), httpHeaders);
		return httpHeaders;
	}

	@SuppressWarnings("unchecked")
	private MediaType resolveContentType(Object content) {
		MediaType contentType = null;
		if (content instanceof byte[]) {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		else if (content instanceof Source) {
			contentType = MediaType.TEXT_XML;
		}
		else if (content instanceof Map) {
			// We need to check separately for MULTIPART as well as URLENCODED simply because
			// MultiValueMap<Object, Object> is actually valid content for serialization
			if (this.isFormData((Map<Object, ?>) content)) {
				if (this.isMultipart((Map<String, ?>) content)) {
					contentType = MediaType.MULTIPART_FORM_DATA;
				}
				else {
					contentType = MediaType.APPLICATION_FORM_URLENCODED;
				}
			}
		}
		if (contentType == null) {
			contentType = new MediaType("application", "x-java-serialized-object");
		}
		return contentType;
	}

	private boolean shouldIncludeRequestBody(HttpMethod httpMethod) {
		return !(CollectionUtils.containsInstance(noBodyHttpMethods, httpMethod));
	}

	private MediaType resolveContentType(String content, Charset charset) {
		return new MediaType("text", "plain", charset);
	}

	private MultiValueMap<Object, Object> convertToMultiValueMap(Map<?, ?> simpleMap) {
		LinkedMultiValueMap<Object, Object> multipartValueMap = new LinkedMultiValueMap<Object, Object>();
		for (Entry<?, ?> entry : simpleMap.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Object[]) {
				value = Arrays.asList((Object[]) value);
			}
			if (value instanceof Collection) {
				multipartValueMap.put(key, new ArrayList<Object>((Collection<?>) value));
			}
			else {
				multipartValueMap.add(key, value);
			}
		}
		return multipartValueMap;
	}

	/**
	 * If all keys are Strings, and some values are not Strings we'll consider
	 * the Map to be multipart/form-data
	 */
	private boolean isMultipart(Map<String, ?> map) {
		for (Object value : map.values()) {
			if (value != null) {
				if (value.getClass().isArray()) {
					value = CollectionUtils.arrayToList(value);
				}
				if (value instanceof Collection) {
					Collection<?> cValues = (Collection<?>) value;
					for (Object cValue : cValues) {
						if (cValue != null && !(cValue instanceof String)) {
							return true;
						}
					}
				}
				else if (!(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * If all keys and values are Strings, we'll consider the Map to be form data.
	 */
	private boolean isFormData(Map<Object, ?> map) {
		for (Object key : map.keySet()) {
			if (!(key instanceof String)) {
				return false;
			}
		}
		return true;
	}

	private HttpMethod determineHttpMethod(Message<?> requestMessage) {
		Object httpMethod = this.httpMethodExpression.getValue(this.evaluationContext, requestMessage);
		Assert.state(httpMethod != null && (httpMethod instanceof String || httpMethod instanceof HttpMethod),
				"'httpMethodExpression' evaluation must result in an 'HttpMethod' enum or its String representation, " +
						"not: " + (httpMethod == null ? "null" : httpMethod.getClass()));
		if (httpMethod instanceof HttpMethod) {
			return (HttpMethod) httpMethod;
		}
		else {
			try {
				return HttpMethod.valueOf((String) httpMethod);
			}
			catch (Exception e) {
				throw new IllegalStateException("The 'httpMethodExpression' returned an invalid HTTP Method value: "
						+ httpMethod);
			}
		}
	}

	private Object determineExpectedResponseType(Message<?> requestMessage) {
		Object expectedResponseType = null;
		if (this.expectedResponseTypeExpression != null) {
			expectedResponseType = this.expectedResponseTypeExpression.getValue(this.evaluationContext, requestMessage);
		}
		if (expectedResponseType != null) {
			Assert.state(expectedResponseType instanceof Class<?>
							|| expectedResponseType instanceof String
							|| expectedResponseType instanceof ParameterizedTypeReference,
					"'expectedResponseType' can be an instance of 'Class<?>', 'String' " +
							"or 'ParameterizedTypeReference<?>'; " +
							"evaluation resulted in a" + expectedResponseType.getClass() + ".");
			if (expectedResponseType instanceof String && StringUtils.hasText((String) expectedResponseType)) {
				try {
					expectedResponseType = ClassUtils.forName((String) expectedResponseType,
							getApplicationContext().getClassLoader());
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException("Cannot load class for name: " + expectedResponseType, e);
				}
			}
		}
		return expectedResponseType;
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> determineUriVariables(Message<?> requestMessage) {
		Map<String, ?> expressions;

		EvaluationContext evaluationContextToUse = this.evaluationContext;
		if (this.uriVariablesExpression != null) {
			Object expressionsObject = this.uriVariablesExpression.getValue(this.evaluationContext, requestMessage);
			Assert.state(expressionsObject instanceof Map,
					"The 'uriVariablesExpression' evaluation must result in a 'Map'.");
			expressions = (Map<String, ?>) expressionsObject;
			if (!this.trustedSpel) {
				evaluationContextToUse = this.simpleEvaluationContext;
			}
		}
		else {
			expressions = this.uriVariableExpressions;
		}

		return ExpressionEvalMap.from(expressions)
				.usingEvaluationContext(evaluationContextToUse)
				.withRoot(requestMessage)
				.build();

	}

}
