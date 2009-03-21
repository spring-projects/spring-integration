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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.net.URL;

import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyMessageHolder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * An outbound endpoint that maps a request Message to an {@link HttpRequest},
 * executes that request, and then maps the response to a reply Message.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class HttpOutboundEndpoint extends AbstractReplyProducingMessageHandler {

	private volatile OutboundRequestMapper requestMapper;

	private volatile HttpRequestExecutor requestExecutor = new SimpleHttpRequestExecutor();


	/**
	 * Create an HttpOutboundEndpoint with no default URL.
	 */
	public HttpOutboundEndpoint() {
		this.requestMapper = new DefaultOutboundRequestMapper();
	}

	/**
	 * Create an HttpOutboundEndpoint that will send requests to the provided
	 * URL by default. If a Message contains a valid value for the 
	 * {@link HttpHeaders#REQUEST_URL} header, that will take precedence.
	 * If a custom {@link OutboundRequestMapper} instance is registered
	 * through the {@link #setRequestMapper(OutboundRequestMapper)} method,
	 * this default URL will not be used.
	 */
	public HttpOutboundEndpoint(URL defaultUrl) {
		this.requestMapper = new DefaultOutboundRequestMapper(defaultUrl);
	}


	/**
	 * Specify an {@link OutboundRequestMapper} implementation to map from
	 * Messages to outbound {@link HttpRequest} objects. The default
	 * implementation is {@link DefaultOutboundRequestMapper}.
	 */
	public void setRequestMapper(OutboundRequestMapper requestMapper) {
		Assert.notNull(requestMapper, "requestMapper must not be null");
		this.requestMapper = requestMapper;
	}

	/**
	 * Specify the {@link HttpRequestExecutor} to use for executing the
	 * {@link HttpRequest} instances at runtime. The default implementation
	 * is {@link SimpleHttpRequestExecutor}.
	 */
	public void setRequestExecutor(HttpRequestExecutor requestExecutor) {
		Assert.notNull(requestExecutor, "requestExecutor must not be null");
		this.requestExecutor = requestExecutor;
	}

	@Override
	protected void handleRequestMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder) {
		try {
			HttpRequest request = this.requestMapper.fromMessage(requestMessage);
			HttpResponse response = this.requestExecutor.executeRequest(request);
			Object reply = this.createReplyFromResponse(response);
			replyMessageHolder.set(reply);
		}
		catch (Exception e) {
			throw new MessageHandlingException(requestMessage, "failed to execute HTTP request", e);
		}
	}

	private Object createReplyFromResponse(HttpResponse response) throws Exception {
		InputStream responseBody = response.getBody();
		Assert.notNull(responseBody, "received null response body");
		String contentType = response.getFirstHeader("Content-Type");
		if (contentType != null && contentType.startsWith("application/x-java-serialized-object")) {
			// may be either a payload or a serialized Message instance
			return this.deserializePayload(responseBody);
		}
		ByteArrayOutputStream responseByteStream = new ByteArrayOutputStream();
		FileCopyUtils.copy(responseBody, responseByteStream);
		if (contentType != null && contentType.startsWith("text")) {
			String charsetName = this.getCharsetName(response);
			if (charsetName == null) {
				charsetName = "ISO-8859-1";
			}
			return responseByteStream.toString(charsetName);
		}
		return responseByteStream.toByteArray();
	}

	private String getCharsetName(HttpResponse httpResponse) {
		String contentType = httpResponse.getFirstHeader("Content-Type");
		if (contentType != null) {
			int beginIndex = contentType.indexOf("charset=");
			if (beginIndex != -1) {
				return contentType.substring(beginIndex + "charset=".length()).trim();
			}
		}
		return null;
	}

	private Object deserializePayload(InputStream responseBody) throws IOException, ClassNotFoundException {
		ObjectInputStream objectStream = null;
		try {
			objectStream = new ObjectInputStream(responseBody);
			return objectStream.readObject();
		}
		catch (ObjectStreamException e) {
			throw new IllegalArgumentException("failed to deserialize response", e);
		}
		finally {
			try {
				objectStream.close();
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

}
