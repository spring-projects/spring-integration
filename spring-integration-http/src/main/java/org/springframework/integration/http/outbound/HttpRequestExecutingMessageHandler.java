/*
 * Copyright 2002-2014 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link MessageHandler} implementation that executes HTTP requests by delegating
 * to a {@link RestTemplate} instance. If the 'expectReply' flag is set to true (the default)
 * then a reply Message will be generated from the HTTP response. If that response contains
 * a body, it will be used as the reply Message's payload. Otherwise the reply Message's
 * payload will contain the response status as an instance of the {@link HttpStatus} enum.
 * When there is a response body, the {@link HttpStatus} enum instance will instead be
 * copied to the MessageHeaders of the reply. In both cases, the response headers will
 * be mapped to the reply Message's headers by this handler's {@link HeaderMapper} instance.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Wallace Wadge
 * @since 2.0
 */
public class HttpRequestExecutingMessageHandler extends AbstractReplyProducingMessageHandler {

	private final Map<String, Expression> uriVariableExpressions = new HashMap<String, Expression>();

	private final RestTemplate restTemplate;

	private volatile StandardEvaluationContext evaluationContext;

	private final Expression uriExpression;

	private volatile boolean encodeUri = true;

	private volatile Expression httpMethodExpression = new LiteralExpression(HttpMethod.POST.name());

	private volatile boolean expectReply = true;

	private volatile Expression expectedResponseTypeExpression;

	private volatile boolean extractPayload = true;

	private volatile boolean extractPayloadExplicitlySet = false;

	private volatile String charset = "UTF-8";

	private volatile boolean transferCookies = false;

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private volatile Expression uriVariablesExpression;



	/**
	 * Create a handler that will send requests to the provided URI.
	 *
	 * @param uri The URI.
	 */
	public HttpRequestExecutingMessageHandler(URI uri) {
		this(uri.toString());
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 *
	 * @param uri The URI.
	 */
	public HttpRequestExecutingMessageHandler(String uri) {
		this(uri, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI Expression.
	 *
	 * @param uriExpression The URI expression.
	 */
	public HttpRequestExecutingMessageHandler(Expression uriExpression) {
		this(uriExpression, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate
	 * @param uri The URI.
	 * @param restTemplate The rest template.
	 */
	public HttpRequestExecutingMessageHandler(String uri, RestTemplate restTemplate) {
		this(new LiteralExpression(uri), restTemplate);
		/*
		 *  We'd prefer to do this assertion first, but the compiler doesn't allow it. However,
		 *  it's safe because the literal expression simply wraps the String variable, even
		 *  when null.
		 */
		Assert.hasText(uri, "URI is required");
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate
	 * @param uriExpression A SpEL Expression that can be resolved against the message object and
	 * {@link BeanFactory}.
	 * @param restTemplate The rest template.
	 */
	public HttpRequestExecutingMessageHandler(Expression uriExpression, RestTemplate restTemplate) {
		Assert.notNull(uriExpression, "URI Expression is required");
		this.restTemplate = (restTemplate == null ? new RestTemplate() : restTemplate);
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
	 * Specify the SpEL {@link Expression} to determine {@link HttpMethod} dynamically
	 *
	 * @param httpMethodExpression The method expression.
	 */
	public void setHttpMethodExpression(Expression httpMethodExpression) {
		Assert.notNull(httpMethodExpression, "'httpMethodExpression' must not be null");
		this.httpMethodExpression = httpMethodExpression;
	}

	/**
	 * Specify the {@link HttpMethod} for requests. The default method will be POST.
	 *
	 * @param httpMethod The method.
	 */
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethodExpression = new LiteralExpression(httpMethod.name());
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body. Otherwise the Message instance itself
	 * will be serialized. The default value is <code>true</code>.
	 *
	 * @param extractPayload true if the payload should be extracted.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
		this.extractPayloadExplicitlySet = true;
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 *
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 *
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
		this.expectedResponseTypeExpression = new LiteralExpression(expectedResponseType.getName());
	}

	/**
	 * Specify the {@link Expression} to determine the type for the expected response
	 * The returned value of the expression could be an instance of {@link Class} or
	 * {@link String} representing a fully qualified class name
	 *
	 * @param expectedResponseTypeExpression The expected response type expression.
	 *
	 * Also see {@link #setExpectedResponseTypeExpression(Expression)}
	 */
	public void setExpectedResponseTypeExpression(Expression expectedResponseTypeExpression) {
		this.expectedResponseTypeExpression = expectedResponseTypeExpression;
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 *
	 * @param errorHandler The error handler.
	 *
	 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.restTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 *
	 * @param messageConverters The message converters.
	 *
	 * @see RestTemplate#setMessageConverters(java.util.List)
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.restTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * @param headerMapper The header mapper.
	 *
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 *
	 * @param requestFactory The request factory.
	 *
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restTemplate.setRequestFactory(requestFactory);
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

	@Override
	public String getComponentType() {
		return (this.expectReply ? "http:outbound-gateway" : "http:outbound-channel-adapter");
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());

		ConversionService conversionService = this.getConversionService();
		if (conversionService == null){
			conversionService = new GenericConversionService();
		}
		if (conversionService instanceof ConverterRegistry){
			ConverterRegistry converterRegistry =
					(ConverterRegistry) conversionService;

			converterRegistry.addConverter(new ClassToStringConverter());
			converterRegistry.addConverter(new ObjectToStringConverter());

			this.evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		else {
			logger.warn("ConversionService is not an instance of ConverterRegistry therefore" +
					"ClassToStringConverter and ObjectToStringConverter will not be registered");
		}
	}

	private class ClassToStringConverter implements Converter<Class<?>, String> {
		@Override
		public String convert(Class<?> source) {
			return source.getName();
		}
	}

	/**
	 *  Spring 3.0.7.RELEASE unfortunately does not trigger the ClassToStringConverter.
	 *  Therefore, this converter will also test for Class instances and do a
	 *  respective type conversion.
	 *
	 */
	private class ObjectToStringConverter implements Converter<Object, String> {
		@Override
		public String convert(Object source) {
			if (source instanceof Class) {
				return ((Class<?>) source).getName();
			}
			return source.toString();
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String uri = this.uriExpression.getValue(this.evaluationContext, requestMessage, String.class);
		Assert.notNull(uri, "URI Expression evaluation cannot result in null");
		try {
			HttpMethod httpMethod = this.determineHttpMethod(requestMessage);

			if (!this.shouldIncludeRequestBody(httpMethod) && this.extractPayloadExplicitlySet){
				if (logger.isWarnEnabled()){
					logger.warn("The 'extractPayload' attribute has no relevance for the current request since the HTTP Method is '" +
							httpMethod + "', and no request body will be sent for that method.");
				}
			}

			Object expectedResponseType = this.determineExpectedResponseType(requestMessage);

			HttpEntity<?> httpRequest = this.generateHttpRequest(requestMessage, httpMethod);
			Map<String, ?> uriVariables = this.determineUriVariables(requestMessage);
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(uri).buildAndExpand(uriVariables);
			URI realUri = this.encodeUri ? uriComponents.toUri() : new URI(uriComponents.toUriString());
			ResponseEntity<?> httpResponse;
			if (expectedResponseType instanceof ParameterizedTypeReference<?>) {
				httpResponse = this.restTemplate.exchange(realUri, httpMethod, httpRequest, (ParameterizedTypeReference<?>) expectedResponseType);
			}
			else {
				httpResponse = this.restTemplate.exchange(realUri, httpMethod, httpRequest, (Class<?>) expectedResponseType);
			}
			if (this.expectReply) {
				HttpHeaders httpHeaders = httpResponse.getHeaders();
				Map<String, Object> headers = this.headerMapper.toHeaders(httpHeaders);
				if (this.transferCookies) {
					this.doConvertSetCookie(headers);
				}
				AbstractIntegrationMessageBuilder<?> replyBuilder = null;
				if (httpResponse.hasBody()) {
					Object responseBody = httpResponse.getBody();
					replyBuilder = (responseBody instanceof Message<?>) ?
							this.getMessageBuilderFactory().fromMessage((Message<?>) responseBody) : this.getMessageBuilderFactory().withPayload(responseBody);

				}
				else {
					replyBuilder = this.getMessageBuilderFactory().withPayload(httpResponse);
				}
				replyBuilder.setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE, httpResponse.getStatusCode());
				return replyBuilder.copyHeaders(headers).build();
			}
			return null;
		}
		catch (MessagingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessageHandlingException(requestMessage, "HTTP request execution failed for URI [" + uri + "]", e);
		}
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

	private HttpEntity<?> generateHttpRequest(Message<?> message, HttpMethod httpMethod) throws Exception {
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
			return new HttpEntity<Object>(httpHeaders);
		}
		// otherwise, we are creating a request with a body and need to deal with the content-type header as well
		if (httpHeaders.getContentType() == null) {
			MediaType contentType = (payload instanceof String) ? this.resolveContentType((String) payload, this.charset)
					: this.resolveContentType(payload);
			httpHeaders.setContentType(contentType);
		}
		if (MediaType.APPLICATION_FORM_URLENCODED.equals(httpHeaders.getContentType()) ||
				MediaType.MULTIPART_FORM_DATA.equals(httpHeaders.getContentType())) {
			if (!(payload instanceof MultiValueMap)) {
				payload = this.convertToMultiValueMap((Map<?,?>) payload);
			}
		}
		return new HttpEntity<Object>(payload, httpHeaders);
	}

	private HttpEntity<?> createHttpEntityFromMessage(Message<?> message, HttpMethod httpMethod) {
		HttpHeaders httpHeaders = mapHeaders(message);
		if (shouldIncludeRequestBody(httpMethod)) {
			httpHeaders.setContentType(new MediaType("application", "x-java-serialized-object"));
			return new HttpEntity<Object>(message, httpHeaders);
		}
		return new HttpEntity<Object>(httpHeaders);
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
				if (this.isMultipart((Map<String, ?>)content)) {
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
		return !HttpMethod.GET.equals(httpMethod);
	}

	private MediaType resolveContentType(String content, String charset) {
		return new MediaType("text", "plain", Charset.forName(charset));
	}

	private MultiValueMap<Object, Object> convertToMultiValueMap(Map<?, ?> simpleMap) {
		LinkedMultiValueMap<Object, Object> multipartValueMap = new LinkedMultiValueMap<Object, Object>();
		for (Object key : simpleMap.keySet()) {
			Object value = simpleMap.get(key);
			if (value instanceof Object[]) {
				Object[] valueArray = (Object[]) value;
				value = Arrays.asList(valueArray);
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
		for (String key : map.keySet()) {
			Object value = map.get(key);
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
		for (Object	 key : map.keySet()) {
			if (!(key instanceof String)) {
				return false;
			}
		}
		return true;
	}

	private HttpMethod determineHttpMethod(Message<?> requestMessage) {
		String strHttpMethod = httpMethodExpression.getValue(this.evaluationContext, requestMessage, String.class);
		Assert.isTrue(StringUtils.hasText(strHttpMethod) && !Arrays.asList(HttpMethod.values()).contains(strHttpMethod),
				"The 'httpMethodExpression' returned an invalid HTTP Method value: " + strHttpMethod);
		return HttpMethod.valueOf(strHttpMethod);
	}

	private Object determineExpectedResponseType(Message<?> requestMessage) throws Exception{
		Object expectedResponseType = null;
		if (this.expectedResponseTypeExpression != null){
			expectedResponseType = this.expectedResponseTypeExpression.getValue(this.evaluationContext, requestMessage);
		}
		if (expectedResponseType != null) {
			Assert.isTrue(expectedResponseType instanceof Class<?>
					|| expectedResponseType instanceof String
					|| expectedResponseType instanceof ParameterizedTypeReference,
					"'expectedResponseType' can be an instance of 'Class<?>', 'String' or 'ParameterizedTypeReference<?>'.");
			if (expectedResponseType instanceof String && StringUtils.hasText((String) expectedResponseType)){
				expectedResponseType = ClassUtils.forName((String) expectedResponseType, ClassUtils.getDefaultClassLoader());
			}
		}
		return expectedResponseType;
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> determineUriVariables(Message<?> requestMessage) {
		Map<String, ?> expressions;

		if (this.uriVariablesExpression != null) {
			expressions = this.uriVariablesExpression.getValue(this.evaluationContext, requestMessage, Map.class);
		}
		else {
			expressions = this.uriVariableExpressions;
		}

		return ExpressionEvalMap.from(expressions)
					.usingEvaluationContext(this.evaluationContext)
					.withRoot(requestMessage)
					.build();

	}

}
