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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.integration.message.MessageTimeoutException;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.View;

/**
 * An inbound endpoint for handling an HTTP request and generating a response.
 * <p/>
 * By default GET and POST requests are accepted, but the 'supportedMethods'
 * property may be set to include others or limit the options (e.g. POST only).
 * By default the request will be converted to a Message payload according to
 * the rules of the {@link DefaultRequestMapper}.
 * <p/>
 * To customize the mapping of the request to the Message payload, provide
 * a reference to a {@link RequestMapper} implementation to the
 * {@link #setRequestMapper(RequestMapper)} method.
 * <p/>
 * The value for {@link #expectReply} is <code>false</code> by default.
 * This means that as soon as the Message is created and passed to the
 * {@link #setRequestChannel(org.springframework.integration.core.MessageChannel) request channel},
 * a response will be generated. If a {@link #setView(View) view} has been
 * provided, it will be invoked to render the response, and it will have
 * access to the request message in the model map. The corresponding key
 * in that map is determined by the {@link #requestKey} property (with a
 * default of "requestMessage"). If no view is provided, and the 'expectReply'
 * value is <code>false</code> then a simple OK status response will be issued.
 * <p/>
 * To handle request-reply scenarios, set the 'expectReply' flag to
 * <code>true</code>. By default, the reply Message's payload will be
 * extracted prior to generating a response. The payload must be either
 * a String, a byte array, or a Serializable object. To have the entire
 * serialized Message written as the response body, switch the
 * {@link #extractReplyPayload} value to <code>false</code>.
 * <p/>
 * In the request-reply case, if a 'view' is provided, the response will
 * not be generated directly from the reply Message or its extracted payload.
 * Instead, the model map will be passed to that view, and it will contain
 * either the reply Message or payload depending on the value of
 * {@link #extractReplyPayload}. The corresponding key in the map will be
 * determined by the {@link #replyKey} property (with a default of "reply").
 * The map will also contain the original request Message as described above. 
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class HttpInboundEndpoint extends SimpleMessagingGateway implements HttpRequestHandler {

	private static final String DEFAULT_REQUEST_KEY = "requestMessage";

	private static final String DEFAULT_REPLY_KEY = "reply";


	private volatile List<String> supportedMethods = Arrays.asList("GET", "POST");

	private volatile boolean expectReply;

	private volatile RequestMapper requestMapper = new DefaultRequestMapper();

	private volatile boolean extractReplyPayload = true;

	private volatile View view;

	private volatile String requestKey = DEFAULT_REQUEST_KEY;

	private volatile String replyKey = DEFAULT_REPLY_KEY; 


	/**
	 * Specify the supported request methods for this endpoint.
	 * By default, only GET and POST are supported.
	 */
	public void setSupportedMethods(String... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported method is required");
		for (int i = 0; i < supportedMethods.length; i++) {
			supportedMethods[i] = supportedMethods[i].trim().toUpperCase();
		}
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
	 * Specify a {@link RequestMapper} implementation to map from the
	 * inbound HTTP request to a Message. The default implementation
	 * is {@link DefaultRequestMapper}.
	 */
	public void setRequestMapper(RequestMapper requestMapper) {
		Assert.notNull(requestMapper, "requestMapper must not be null");
		this.requestMapper = requestMapper;
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

	/**
	 * Specify the key to be used when storing the request Message in the model
	 * map. This is only necessary when a {@link #setView(View) view} has been
	 * provided for rendering the response. The default key is "requestMessage".
	 */
	public void setRequestKey(String requestKey) {
		this.requestKey = (requestKey != null) ? requestKey : DEFAULT_REQUEST_KEY;
	}

	/**
	 * Specify the key to be used when storing the reply Message or payload in
	 * the model map. This is only necessary when a {@link #setView(View) view}
	 * has been provided for rendering the response. The default key is "reply".
	 */
	public void setReplyKey(String replyKey) {
		this.replyKey = (replyKey != null) ? replyKey : DEFAULT_REPLY_KEY;
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!this.supportedMethods.contains(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}
		try {
			Message<?> requestMessage = this.requestMapper.mapRequest(request);
			Object reply = this.handleRequestMessage(requestMessage);
			this.generateResponse(requestMessage, reply, request, response);
		}
		catch (ResponseStatusCodeException e) {
			response.setStatus(e.getStatusCode());
		}
		catch (ServletException e) {
			throw e;
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
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
			if (reply == null) {
				throw new MessageTimeoutException(requestMessage,
						"failed to handle Message within specified timeout value");
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
			model.put(this.requestKey, requestMessage);
			if (reply != null) {
				model.put(this.replyKey, reply);
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

}
