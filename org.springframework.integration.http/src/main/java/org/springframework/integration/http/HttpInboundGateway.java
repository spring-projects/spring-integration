/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.web.HttpRequestHandler;

/**
 * @author Mark Fisher
 * @since 1.0.2
 */
public class HttpInboundGateway extends SimpleMessagingGateway implements HttpRequestHandler {

	private volatile boolean extractPayload = true;


	/**
	 * Specify whether the inbound request's content should be passed as
	 * the payload of the Message. If this is set to 'false', the entire
	 * request will be sent as the payload. Otherwise, for a GET request
	 * the parameter map will be the payload, and for a POST request the
	 * body will be extracted. In the case of a POST request, the type of
	 * the payload depends on the Content-Type of the request.
	 * The default value is 'true'. 
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Message<?> message = null;
		if (this.extractPayload) {
			message = this.createMessageFromRequestPayload(request);
		}
		else {
			message = MessageBuilder.withPayload(request).build();
		}
		Object result = this.sendAndReceive(message);
		if (result instanceof String) {
			response.getWriter().print((String) result);
			response.flushBuffer();
		}
		else if (result instanceof byte[]) {
			response.getOutputStream().write((byte[]) result);
			response.flushBuffer();
		}
		else {
			throw new ServletException("invalid response type [" + result.getClass().getName() + "]"); 
		}
	}

	private Message<?> createMessageFromRequestPayload(HttpServletRequest request) throws IOException {
		Message<?> message = null;
		String contentType = request.getContentType();
		if (request.getMethod().equals("GET")) {
			if (logger.isDebugEnabled()) {
				logger.debug("received GET request, using parameter map as payload");
			}
			MessageBuilder<?> builder = MessageBuilder.withPayload(request.getParameterMap());
			this.populateHeaders(request, builder, false);
			message = builder.build();
		}
		else if (request.getMethod().equals("POST")) {
			Object payload = null;
			if (contentType != null && contentType.startsWith("text")) {
				if (logger.isDebugEnabled()) {
					logger.debug("received POST request, creating payload with text content");
				}
				StringBuilder sb = new StringBuilder();
				BufferedReader reader = request.getReader();
				String line = reader.readLine();
				while (line != null) {
					sb.append(line);
					line = reader.readLine();
				}
				payload = sb.toString();
			}
			else {
				InputStream stream = request.getInputStream();
				int length = request.getContentLength();
				if (length == -1) {
					length = 1024;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("received POST request, creating byte array payload with content lenth: " + length);
				}
				byte[] bytes = new byte[length];
				stream.read(bytes, 0, length);
				payload = bytes;
			}
			MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
			this.populateHeaders(request, builder, true);
			message = builder.build();
		}
		else {
			throw new UnsupportedOperationException("unsupported method: " + request.getMethod());
		}
		return message;
	}

	@SuppressWarnings("unchecked")
	private void populateHeaders(HttpServletRequest request, MessageBuilder<?> builder, boolean includeParameters) {
		Enumeration<?> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = (String) headerNames.nextElement();
				Enumeration<?> headerEnum = request.getHeaders(headerName);
				if (headerEnum != null) {
					List<Object> headers = new ArrayList<Object>();
					while (headerEnum.hasMoreElements()) {
						headers.add(headerEnum.nextElement());
					}
					if (headers.size() == 1) {
						builder.setHeader(headerName, headers.get(0));
					}
					else if (headers.size() > 1) {
						builder.setHeader(headerName, headers);
					}
				}
			}
		}
		if (includeParameters) {
			builder.copyHeaders(request.getParameterMap());
		}
	}

}
