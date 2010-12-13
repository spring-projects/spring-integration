/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
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
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

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
 * @since 2.0
 */
public class HttpRequestExecutingMessageHandler extends AbstractReplyProducingMessageHandler {

	private static final ExpressionParser PARSER = new SpelExpressionParser();


	private final String uri;

	private volatile HttpMethod httpMethod = HttpMethod.POST;

	private volatile boolean expectReply = true;

	private volatile Class<?> expectedResponseType;

	private volatile boolean extractPayload = true;

	private volatile String charset = "UTF-8";

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private final Map<String, Expression> uriVariableExpressions = new HashMap<String, Expression>();

	private final RestTemplate restTemplate = new RestTemplate();

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();


	/**
	 * Create a handler that will send requests to the provided URI.
	 */
	public HttpRequestExecutingMessageHandler(URI uri) {
		this(uri.toString());
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 */
	public HttpRequestExecutingMessageHandler(String uri) {
		Assert.hasText(uri, "URI is required");
		this.restTemplate.getMessageConverters().add(0, new SerializingHttpMessageConverter());
		this.uri = uri;
	}


	/**
	 * Specify the {@link HttpMethod} for requests. The default method will be POST.
	 */
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body. Otherwise the Message instance itself
	 * will be serialized. The default value is <code>true</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Specify the expected response type for the REST request.
	 * If this is null (the default), only the status code will be returned
	 * as the reply Message payload. To take advantage of the HttpMessageConverters
	 * registered on this adapter, provide a different type).
	 */
	public void setExpectedResponseType(Class<?> expectedResponseType) {
		this.expectedResponseType = expectedResponseType;
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.restTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 * @see RestTemplate#setMessageConverters(java.util.List)
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.restTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restTemplate.setRequestFactory(requestFactory);
	}
	
	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 */
	public void setUriVariableExpressions(Map<String, String> uriVariableExpressions) {
		synchronized (this.uriVariableExpressions) {
			this.uriVariableExpressions.clear();
			if (!CollectionUtils.isEmpty(uriVariableExpressions)) {
				for (Map.Entry<String, String> entry : uriVariableExpressions.entrySet()) {
					this.uriVariableExpressions.put(entry.getKey(), PARSER.parseExpression(entry.getValue()));
				}
			}
		}
	}

	@Override
	public void onInit() {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		ConversionService conversionService = this.getConversionService();
		if (conversionService != null) {
			this.evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		try {
			Map<String, Object> uriVariables = new HashMap<String, Object>();
			for (Map.Entry<String, Expression> entry : this.uriVariableExpressions.entrySet()) {
				Object value = entry.getValue().getValue(this.evaluationContext, requestMessage, String.class);
				uriVariables.put(entry.getKey(), value);
			}
			HttpEntity<?> httpRequest = this.generateHttpRequest(requestMessage);
			ResponseEntity<?> httpResponse = this.restTemplate.exchange(this.uri, this.httpMethod, httpRequest, this.expectedResponseType, uriVariables);
			if (this.expectReply) {
				Map<String, ?> headers = this.headerMapper.toHeaders(httpResponse.getHeaders());
				if (httpResponse.hasBody()) {
					Object responseBody = httpResponse.getBody();
					MessageBuilder<?> replyBuilder = (responseBody instanceof Message<?>) ?
							MessageBuilder.fromMessage((Message<?>) responseBody) : MessageBuilder.withPayload(responseBody);
					replyBuilder.setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE, httpResponse.getStatusCode());
					return replyBuilder.copyHeaders(headers).build();
				}
				else {
					return MessageBuilder.withPayload(httpResponse.getStatusCode()).copyHeaders(headers).build();
				}
			}
			return null;
		}
		catch (MessagingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessageHandlingException(requestMessage, "HTTP request execution failed for URI [" + this.uri + "]", e);
		}
	}

	private HttpEntity<?> generateHttpRequest(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		return (this.extractPayload) ? this.createHttpEntityWithPayloadAsBody(message)
				: this.createHttpEntityWithMessageAsBody(message);
	}

	@SuppressWarnings({ "unchecked", "rawtypes"})
	private HttpEntity<?> createHttpEntityWithPayloadAsBody(Message<?> requestMessage) {
		if (requestMessage.getPayload() instanceof HttpEntity<?>) {
			return (HttpEntity<?>) requestMessage.getPayload();
		}
		HttpHeaders httpHeaders = new HttpHeaders();
		this.headerMapper.fromHeaders(requestMessage.getHeaders(), httpHeaders);
		Object payload = requestMessage.getPayload();
		if (httpHeaders.getContentType() == null) {
			MediaType contentType = (payload instanceof String) ? this.resolveContentType((String) payload, this.charset)
					: this.resolveContentType(payload);
			httpHeaders.setContentType(contentType);
		}
		if (MediaType.APPLICATION_FORM_URLENCODED.equals(httpHeaders.getContentType()) ||
				MediaType.MULTIPART_FORM_DATA.equals(httpHeaders.getContentType())) {
			if (!(payload instanceof MultiValueMap)) {
				payload = this.convertToMultiValueMap((Map) payload);
			}
		}
		if (HttpMethod.POST.equals(this.httpMethod) || HttpMethod.PUT.equals(this.httpMethod)) {
			return new HttpEntity<Object>(payload, httpHeaders);
		}
		return new HttpEntity<Object>(httpHeaders);
	}

	private HttpEntity<Object> createHttpEntityWithMessageAsBody(Message<?> requestMessage) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "x-java-serialized-object"));
		return new HttpEntity<Object>(requestMessage, headers);
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

	private MediaType resolveContentType(String content, String charset) {
		return new MediaType("text", "plain", Charset.forName(charset));
	}

	private MultiValueMap<Object, Object> convertToMultiValueMap(Map<Object, Object> simpleMap) {
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

}
