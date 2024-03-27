/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestHandler;

/**
 * Inbound Messaging Gateway that handles HTTP Requests. May be configured as a bean in the Application Context and
 * delegated to from a simple HttpRequestHandlerServlet in <code>web.xml</code> where the servlet and bean both have the
 * same name. If the {@link #isExpectReply()} property is set to true, a response can generated from a reply Message.
 * Otherwise, the gateway will play the role of a unidirectional Channel Adapter with a simple status-based response
 * (e.g. 200 OK).
 * <p>
 * The default supported request methods are GET and POST, but the list of values can be configured with the
 * {@link RequestMapping#getMethods()} property.
 * The payload generated from a GET request (or HEAD or OPTIONS if supported) will
 * be a {@link org.springframework.util.MultiValueMap} containing the parameter values.
 * For a request containing a body (e.g. a POST), the type of the payload is determined
 * by the {@link #setRequestPayloadTypeClass(Class)} request payload type}.
 * <p>
 * If the HTTP request is a multipart and a "multipartResolver" bean has been defined in the context, then it will be
 * converted by the
 * {@link org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter} as long as the default
 * message converters have not
 * been overwritten (although providing a customized instance of the Multipart-aware converter is also an option).
 * <p>
 * By default a number of {@link HttpMessageConverter}s are already configured. The list can be overridden by calling
 * the {@link #setMessageConverters(List)} method.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public class HttpRequestHandlingMessagingGateway extends HttpRequestHandlingEndpointSupport
		implements HttpRequestHandler {

	private volatile boolean convertExceptions;

	public HttpRequestHandlingMessagingGateway() {
		this(true);
	}

	public HttpRequestHandlingMessagingGateway(boolean expectReply) {
		super(expectReply);
	}

	/**
	 * Flag to determine if conversion and writing out of message handling exceptions should be attempted (default
	 * false, in which case they will simply be re-thrown). If the flag is true and no message converter can convert the
	 * exception a new exception will be thrown.
	 * @param convertExceptions the flag to set
	 */
	public void setConvertExceptions(boolean convertExceptions) {
		this.convertExceptions = convertExceptions;
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel. If this gateway's
	 * 'expectReply' property is true, it will also generate a response from the reply Message once received. That
	 * response will be written by the {@link HttpMessageConverter}s.
	 */
	@Override
	public final void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws IOException {

		Object responseContent = null;
		Message<?> responseMessage;

		ServletServerHttpRequest request = prepareRequest(servletRequest);
		ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

		RequestEntity<Object> httpEntity = prepareRequestEntity(request);

		try {
			responseMessage = doHandleRequest(servletRequest, httpEntity);
			if (responseMessage != null) {
				responseContent = setupResponseAndConvertReply(response, responseMessage);
			}
		}
		catch (Exception ex) {
			responseContent = handleExceptionInternal(ex);
		}

		if (responseContent != null) {

			if (responseContent instanceof HttpStatus) {
				response.setStatusCode((HttpStatus) responseContent);
			}
			else {
				if (responseContent instanceof ResponseEntity) {
					ResponseEntity<?> responseEntity = (ResponseEntity<?>) responseContent;
					responseContent = responseEntity.getBody();
					response.setStatusCode(responseEntity.getStatusCode());

					HttpHeaders outputHeaders = response.getHeaders();
					HttpHeaders entityHeaders = responseEntity.getHeaders();

					entityHeaders.entrySet()
							.stream()
							.filter(entry -> !outputHeaders.containsKey(entry.getKey()))
							.forEach(entry -> outputHeaders.put(entry.getKey(), entry.getValue()));
				}

				if (responseContent != null) {
					writeResponse(responseContent, response, request.getHeaders().getAccept());
				}
				else {
					response.flush();
				}
			}
		}
		else {
			setStatusCodeIfNeeded(response, httpEntity);
		}
	}

	private Object handleExceptionInternal(Exception ex) throws IOException {
		if (this.convertExceptions && isExpectReply()) {
			return ex;
		}
		else {
			if (ex instanceof IOException) {
				throw (IOException) ex;
			}
			else if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			}
			else {
				throw new MessagingException("error occurred handling HTTP request", ex);
			}
		}
	}

	private void writeResponse(Object content, ServletServerHttpResponse response, List<MediaType> acceptTypesArg)
			throws IOException {

		List<MediaType> acceptTypes = acceptTypesArg;
		if (CollectionUtils.isEmpty(acceptTypes)) {
			acceptTypes = Collections.singletonList(MediaType.ALL);
		}
		for (HttpMessageConverter<?> converter : getMessageConverters()) {
			for (MediaType acceptType : acceptTypes) {
				if (converter.canWrite(content.getClass(), acceptType)) {
					@SuppressWarnings("unchecked")
					HttpMessageConverter<Object> converterToUse = (HttpMessageConverter<Object>) converter;
					converterToUse.write(content, acceptType, response);
					return;
				}
			}
		}
		throw new MessagingException("Could not convert reply: no suitable HttpMessageConverter found for type ["
				+ content.getClass().getName() + "] and accept types [" + acceptTypes + "]");
	}

}
