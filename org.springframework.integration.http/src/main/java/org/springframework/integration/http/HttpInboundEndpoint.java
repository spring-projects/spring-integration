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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.View;

/**
 * @author Mark Fisher
 * @since 1.0.2
 */
public class HttpInboundEndpoint extends SimpleMessagingGateway implements HttpRequestHandler {

	private volatile List<String> supportedMethods = Arrays.asList("GET", "POST");

	private volatile boolean expectReply;

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile View view;


	/**
	 * Specify the supported request methods for this endpoint.
	 * By default, only GET and POST are supported.
	 */
	public void setSupportedMethods(String... supportedMethods) {
		this.supportedMethods = Arrays.asList(supportedMethods);
	}

	/**
	 * Specify whether this endpoint should perform a request/reply
	 * operation. Otherwise, it will only send the message and
	 * immediately generate a response. The default is 'false'.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Specify whether the inbound request's content should be passed as
	 * the payload of the Message. If this is set to 'false', the entire
	 * request will be sent as the payload. Otherwise, for a GET request
	 * the parameter map will be the payload. For other supported request
	 * methods, the body will be extracted, and the type of the payload
	 * depends on the Content-Type of the request.
	 * The default value is 'true'. 
	 */
	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	/**
	 * Specify whether the reply Message's payload should be passed in
	 * the response. If this is set to 'false', the entire Message will
	 * be sent as bytes. Otherwise, the reply Message payload must be
	 * a String or byte array. If a 'view' has been provided,
	 * the reply value will be sent in the model Map to that View.
	 * If the 'view' is <code>null</code>, the String or byte array
	 * will be written directly to the HTTP response. 
	 * <p>The default value is 'true'.
	 * @see #setView(View) 
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	/**
	 * Specify a {@link View} to be used for rendering the
	 * response. If no View is provided, the reply Message or its
	 * payload will be written directly to the response.
	 * @see #setExtractReplyPayload(boolean)
	 */
	public void setView(View view) {
		this.view = view;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!this.supportedMethods.contains(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}
		try {
			Message<?> requestMessage = this.createRequestMessage(request);
			Object reply = this.handleRequestMessage(requestMessage);
			this.generateResponse(requestMessage, reply, request, response);
		}
		catch (RequiredContentLengthUnavailableException e) {
			response.setStatus(HttpServletResponse.SC_LENGTH_REQUIRED);
		}
	}

	/**
	 * Create a request Message for the provided HTTP request.
	 * @see #setExtractRequestPayload(boolean)
	 */
	private Message<?> createRequestMessage(HttpServletRequest httpRequest) throws IOException {
		if (this.extractRequestPayload) {
			return this.createMessageFromHttpRequestContent(httpRequest);
		}
		else {
			return MessageBuilder.withPayload(httpRequest).build();
		}
	}

	private Message<?> createMessageFromHttpRequestContent(HttpServletRequest request) throws IOException {
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
			else {
				InputStream stream = request.getInputStream();
				int length = request.getContentLength();
				if (length == -1) {
					throw new RequiredContentLengthUnavailableException();
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

	private Object handleRequestMessage(Message<?> requestMessage) {
		Object reply = null;
		if (this.expectReply) {
			if (this.extractReplyPayload) {
				reply = this.sendAndReceive(requestMessage);
			}
			else {
				reply = this.sendAndReceiveMessage(requestMessage);
			}
		}
		else {
			this.send(requestMessage);
		}
		return reply;
	}

	private void generateResponse(Message<?> requestMessage, Object reply,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		if (this.view != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("requestMessage", requestMessage);
			if (reply instanceof Message) {
				model.put("replyMessage", reply);
			}
			else if (reply != null) {
				model.put("replyPayload", reply);
			}
			try {
				this.view.render(model, httpRequest, httpResponse);
			}
			catch (Exception e) {
				throw new ServletException("failed to render view", e);
			}
		}
		else if (reply == null) {
			httpResponse.setStatus(HttpServletResponse.SC_OK);
		}
		else if (reply instanceof String) {
			httpResponse.getWriter().print((String) reply);
			httpResponse.flushBuffer();
		}
		else if (reply instanceof byte[]) {
			byte[] bytes = (byte[]) reply;
			httpResponse.getOutputStream().write(bytes);
			httpResponse.setContentLength(bytes.length);
			httpResponse.flushBuffer();
		}
		else if (reply instanceof Serializable) {
			// either a Serializable payload or the Message itself
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
			objectStream.writeObject(reply);
			objectStream.flush();
			objectStream.close();
			byte[] bytes = byteStream.toByteArray();
			httpResponse.getOutputStream().write(bytes);
			httpResponse.setContentType("application/x-java-serialized-object");
			httpResponse.setContentLength(bytes.length);
			httpResponse.flushBuffer();
		}
		else {
			throw new ServletException("failed to generate HTTP response from reply Message");
		}
	}


	@SuppressWarnings("serial")
	private static class RequiredContentLengthUnavailableException extends RuntimeException {
	}

}
