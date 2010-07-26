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

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;

/**
 * Inbound Messaging Gateway that handles HTTP Requests. May be configured as a bean in the
 * Application Context and delegated to from a simple HttpRequestHandlerServlet in
 * <code>web.xml</code> where the servlet and bean both have the same name. If the
 * {@link #expectReply} property is set to true, a response can generated from a
 * reply Message. Otherwise, the gateway will play the role of a unidirectional
 * Channel Adapter with a simple status-based response (e.g. 200 OK).
 * <p/>
 * The default supported request methods are GET and POST, but the list of values can
 * be configured with the {@link #supportedMethods} property. The payload generated from
 * a GET request (or HEAD or OPTIONS if supported) will be a {@link MultiValueMap} 
 * containing the parameter values. For a request containing a body (e.g. a POST),
 * the type of the payload is determined by the {@link #conversionTargetType} property.
 * <p/>
 * If the HTTP request is a multipart, a {@link MultiValueMap} payload will be generated. If
 * this gateway's {@link #uploadMultipartFiles} property is set to true, any files included
 * in that multipart request will be copied to the temporary directory. The corresponding
 * values for those files within the payload map will be {@link java.io.File} instances.
 * <p/>
 * By default a number of {@link HttpMessageConverter}s are already configured. The list
 * can be overridden by calling the {@link #setMessageConverters(List)} method.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestHandlingMessagingGateway extends HttpRequestHandlingEndpointSupport implements HttpRequestHandler {

	private volatile boolean extractReplyPayload = true;


	public HttpRequestHandlingMessagingGateway() {
		this(true);
	}

	public HttpRequestHandlingMessagingGateway(boolean expectReply) {
		super(expectReply);
	}


	/**
	 * Specify whether the reply Message's payload should be passed in
	 * the response. If this is set to 'false', the entire Message will
	 * be processed by the {@link HttpMessageConverter}s. Otherwise, the
	 * reply Message payload will be processed. The default is 'true'.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload; 
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel.
	 * If this gateway's 'expectReply' property is true, it will also generate a response from
	 * the reply Message once received.
	 */
	public final void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		Object responseContent = super.handleRequest(servletRequest);
		ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
		if (responseContent instanceof Message<?>) {
			this.getHeaderMapper().fromHeaders(((Message<?>) responseContent).getHeaders(), response.getHeaders());
			if (this.extractReplyPayload) {
				responseContent = ((Message<?>) responseContent).getPayload();
			}
		}
		if (responseContent != null) {
			ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
			this.writeResponse(responseContent, response, request.getHeaders().getAccept());
		}
	}

	@SuppressWarnings("unchecked")
	private void writeResponse(Object content, ServletServerHttpResponse response, List<MediaType> acceptTypes) throws IOException {
		for (HttpMessageConverter converter : this.getMessageConverters()) {
			for (MediaType acceptType : acceptTypes) {
				if (converter.canWrite(content.getClass(), acceptType)) {
					converter.write(content, acceptType, response);
					return;
				}
			}
		}
		throw new MessagingException("Could not convert reply: no suitable HttpMessageConverter found for result type [" +
				content.getClass().getName() + "] and content types [" + acceptTypes + "]");
	}

}
