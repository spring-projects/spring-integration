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

	private volatile HttpMethod httpMethod = HttpMethod.POST;

	private boolean expectReply = true;

	private volatile Class<?> expectedResponseType;

	private final DefaultOutboundRequestMapper requestMapper = new DefaultOutboundRequestMapper();

	private final RestTemplate restTemplate = new RestTemplate();

	private ParameterExtractor parameterExtractor = new DefaultParameterExtractor();

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
		this.requestMapper.setHttpMethod(httpMethod);
		this.httpMethod = httpMethod;
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body. Otherwise the Message instance itself
	 * will be serialized. The default value is <code>true</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.requestMapper.setExtractPayload(extractPayload);
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		this.requestMapper.setCharset(charset);
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
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restTemplate.setRequestFactory(requestFactory);
	}
	
	/**
	 * Set the {@link ParameterExtractor} for creating URI parameters from the outbound message.
	 * 
	 * @param parameterExtractor the parameter extractor to set
	 */
	public void setParameterExtractor(ParameterExtractor parameterExtractor) {
		this.parameterExtractor = parameterExtractor;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		try {
			// TODO: allow a boolean flag for treating Map as queryParams vs. uriVariables?
			Map<String, ?> uriVariables = this.parameterExtractor.fromMessage(requestMessage);
			HttpEntity<?> httpRequest = this.requestMapper.fromMessage(requestMessage);
			ResponseEntity<?> httpResponse = this.restTemplate.exchange(this.uri, this.httpMethod, httpRequest, this.expectedResponseType, uriVariables);
			if (this.expectReply) {
				if (httpResponse.hasBody()) {
					Object responseBody = httpResponse.getBody();
					MessageBuilder<?> replyBuilder = (responseBody instanceof Message<?>) ?
							MessageBuilder.fromMessage((Message<?>) responseBody) : MessageBuilder.withPayload(responseBody);
							return replyBuilder.copyHeaders(httpResponse.getHeaders().toSingleValueMap()).build();
				}
				else {
					return MessageBuilder.withPayload(httpResponse.getStatusCode()).copyHeaders(httpResponse.getHeaders().toSingleValueMap()).build();
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

}
