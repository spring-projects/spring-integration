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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * Default implementation of {@link InboundRequestMapper} for inbound HttpServletRequests.
 * The request will be mapped according to the following rules:
 * <ul>
 * <li>For a GET request, the parameter Map will be copied as the payload.
 * The map's keys will be Strings, and the values will be String arrays
 * as described for {@link ServletRequest#getParameterMap()}.</li>
 * <li>For other request types, the request body will be used as the payload
 * and the type will depend on the Content-Type header value. If it
 * begins with "text", a String will be created. If the Content-Type
 * is "application/x-java-serialized-object", the request body will be
 * expected to contain a Serializable Object, and that will be used as
 * the message payload. Otherwise, the payload will be a byte array.
 * The parameter Map values will then be added as Message headers.</li>
 * </ul>
 * In both cases, the original request headers will be passed in the
 * MessageHeaders. Likewise, the following headers will be added:
 * <ul>
 *   <li>{@link HttpHeaders#REQUEST_URL}</li>
 *   <li>{@link HttpHeaders#REQUEST_METHOD}</li>
 *   <li>{@link HttpHeaders#USER_PRINCIPAL} (if available)</li>
 * </ul>
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class DefaultInboundRequestMapper implements InboundRequestMapper {

	private Log logger = LogFactory.getLog(getClass());


	public Message<?> toMessage(HttpServletRequest request) throws Exception {
		Message<?> message = null;
		String contentType = request.getContentType();
		if (request.getMethod().equals("GET")) {
			message = this.createMessageFromGetRequest(request);
		}
		else {
			Object payload = null;
			if (contentType != null && contentType.startsWith("text")) {
				if (logger.isDebugEnabled()) {
					logger.debug("received " + request.getMethod()
							+ " request, creating payload with text content");
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
			else if (contentType != null && contentType.equals("application/x-java-serialized-object")) {
				try {
					payload = new ObjectInputStream(request.getInputStream()).readObject();
				}
				catch (ClassNotFoundException e) {
					throw new ServletException("failed to deserialize Object in request", e);
				}
			}
			else {
				InputStream stream = request.getInputStream();
				int length = request.getContentLength();
				if (length == -1) {
					throw new ResponseStatusCodeException(HttpServletResponse.SC_LENGTH_REQUIRED);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("received " + request.getMethod() + " request, "
							+ "creating byte array payload with content lenth: " + length);
				}
				byte[] bytes = new byte[length];
				stream.read(bytes, 0, length);
				payload = bytes;
			}
			MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
			this.populateHeaders(request, builder, true);
			message = builder.build();
		}
		return message;
	}

	@SuppressWarnings("unchecked")
	private Message<?> createMessageFromGetRequest(HttpServletRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("received GET request, using parameter map as payload");
		}
		Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());
		MessageBuilder<?> builder = MessageBuilder.withPayload(Collections.unmodifiableMap(parameterMap));
		this.populateHeaders(request, builder, false);
		return builder.build();
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
		builder.setHeader(HttpHeaders.REQUEST_URL, request.getRequestURL().toString());
		builder.setHeader(HttpHeaders.REQUEST_METHOD, request.getMethod());
		builder.setHeader(HttpHeaders.USER_PRINCIPAL, request.getUserPrincipal());
	}

}
