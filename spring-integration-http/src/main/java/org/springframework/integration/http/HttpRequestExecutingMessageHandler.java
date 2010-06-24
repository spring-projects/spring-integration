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

package org.springframework.integration.http;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * A {@link MessageHandler} implementation that executes HTTP requests by delegating
 * to a {@link RestTemplate} instance.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestExecutingMessageHandler extends AbstractReplyProducingMessageHandler {

	private final String uri;

	private volatile HttpMethod defaultHttpMethod = HttpMethod.POST;

	private volatile OutboundRequestMapper requestMapper = new DefaultOutboundRequestMapper();

	private boolean expectReply = true;

	private volatile Class<?> expectedResponseType = Object.class;

	private final RestTemplate restTemplate = new RestTemplate();


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
		this.restTemplate.getMessageConverters().add(0, new SerializingHttpMessageConverter());
		this.uri = uri;
	}


	/**
	 * Specify the default {@link HttpMethod}. This will provide a fallback in the case
	 * that a Message does not contain the HTTP method as a header. If this is not
	 * explicitly specified, then the default method will be POST.
	 */
	public void setDefaultHttpMethod(HttpMethod defaultHttpMethod) {
		this.defaultHttpMethod = defaultHttpMethod;
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
	 */
	public void setExpectedResponseType(Class<?> expectedResponseType) {
		this.expectedResponseType = (expectedResponseType != null) ? expectedResponseType : byte[].class;
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
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restTemplate.setRequestFactory(requestFactory);
	}

	/**
	 * Specify the {@link OutboundRequestMapper} implementation to use for mapping a
	 * {@link Message} into an {@link HttpEntity} when executing an HTTP request.
	 * <p>
	 * If not provided explicitly, the default implementation is {@link DefaultOutboundRequestMapper}. 
	 */
	public void setRequestMapper(OutboundRequestMapper requestMapper) {
		Assert.notNull(requestMapper, "requestMapper must not be null");
		this.requestMapper = requestMapper;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		try {
			HttpMethod httpMethod = this.resolveHttpMethod(requestMessage);
			// TODO: allow a boolean flag for treating Map as queryParams vs. uriVariables?
			Map<String, ?> uriVariables = this.determineUriVariables(requestMessage);
			HttpEntity<?> httpRequest = this.requestMapper.fromMessage(requestMessage);
			if (!isWritableRequestMethod(httpMethod) && httpRequest.getBody() != null) {
				httpRequest = new HttpEntity<Object>(null, httpRequest.getHeaders());
			}
			ResponseEntity<?> httpResponse = this.restTemplate.exchange(this.uri, httpMethod, httpRequest, this.expectedResponseType, uriVariables);
			if (this.expectReply) {
				Object responseBody = httpResponse.getBody();
				MessageBuilder<?> replyBuilder = (responseBody instanceof Message<?>) ?
						MessageBuilder.fromMessage((Message<?>) responseBody) : MessageBuilder.withPayload(responseBody);
				return replyBuilder.copyHeaders(httpResponse.getHeaders().toSingleValueMap()).build();
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

	private boolean isWritableRequestMethod(HttpMethod httpMethod) {
		switch (httpMethod) {
			case POST: case PUT: return true;
			default: return false;
		}
	}

	private HttpMethod resolveHttpMethod(Message<?> requestMessage) {
		HttpMethod httpMethod = null;
		Object methodFromMessage = requestMessage.getHeaders().get(HttpHeaders.REQUEST_METHOD);
		if (methodFromMessage instanceof HttpMethod) {
			httpMethod = (HttpMethod) methodFromMessage;
		}
		else if (methodFromMessage instanceof String) {
			httpMethod = HttpMethod.valueOf((String) methodFromMessage);
		}
		else if (methodFromMessage != null) {
			throw new IllegalArgumentException("expected an HttpMethod enum instance or String for " +
					"the REQUEST_METHOD header, but received type: " + methodFromMessage.getClass());
		}
		if (httpMethod == null) {
			httpMethod = this.defaultHttpMethod;
		}
		return httpMethod;
	}

	private Map<String, ?> determineUriVariables(Message<?> requestMessage) {
		Map<String, Object> uriVariables = new HashMap<String, Object>();
		if (requestMessage.getPayload() instanceof Map<?,?>) {
			Map<?,?> payloadMap = (Map<?,?>) requestMessage.getPayload();
			for (Object key : payloadMap.keySet()) {
				if (key instanceof String) {
					System.out.println("adding value for key: " + key);
					uriVariables.put((String) key, payloadMap.get(key).toString());
				}
				else if (logger.isDebugEnabled()) {
					logger.debug("ignoring Map value for non-String key: " + key);
				}
			}
		}
		return uriVariables;
	}

}
