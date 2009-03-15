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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyMessageHolder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Iwein Fuld
 * @since 1.0.2
 */
public class HttpOutboundEndpoint extends AbstractReplyProducingMessageHandler {

	private volatile String charset = "UTF-8";

	private volatile boolean acceptGzipEncoding = true;

	private volatile HttpExchanger exchanger;

	public HttpOutboundEndpoint(String url) {
		this.exchanger = new SimpleHttpExchanger(url);
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '"
				+ charset + "'");
		this.charset = charset;
	}

	/**
	 * Specify the content type to use for sending HTTP requests.
	 * <p>
	 * Default is "application/x-java-serialized-object".
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.exchanger.setContentType(contentType);
	}

	/**
	 * Set whether to accept GZIP encoding, that is, whether to send the HTTP
	 * "Accept-Encoding" header with "gzip" as value.
	 * <p>
	 * Default is "true". Turn this flag off if you do not want GZIP response
	 * compression even if enabled on the HTTP server.
	 */
	public void setAcceptGzipEncoding(boolean acceptGzipEncoding) {
		this.acceptGzipEncoding = acceptGzipEncoding;
	}

	/**
	 * Return whether to accept GZIP encoding, that is, whether to send the HTTP
	 * "Accept-Encoding" header with "gzip" as value.
	 */
	public boolean isAcceptGzipEncoding() {
		return this.acceptGzipEncoding;
	}

	@Override
	protected void handleRequestMessage(Message<?> requestMessage,
			ReplyMessageHolder replyMessageHolder) {
		Object payload = requestMessage.getPayload();
		byte[] bytes = null;
		try {
			if (payload instanceof byte[]) {
				bytes = (byte[]) payload;
			}
			else if (payload instanceof String) {
				bytes = ((String) payload).getBytes(this.charset);
			}
			else if (payload instanceof Serializable) {
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ObjectOutputStream objectStream = new ObjectOutputStream(
						byteStream);
				objectStream.writeObject(payload);
				objectStream.flush();
				objectStream.close();
				bytes = byteStream.toByteArray();
			}
			else {
				throw new IllegalArgumentException(
						"payload must be a byte array, String, or Serializable object");
			}
			ByteArrayOutputStream requestByteStream = new ByteArrayOutputStream();
			requestByteStream.write(bytes);

			ByteArrayOutputStream responseByteStream = new ByteArrayOutputStream();
			InputStream responseBody = exchanger.exchange(requestByteStream);
			FileCopyUtils.copy(responseBody, responseByteStream);
			replyMessageHolder.set(responseByteStream.toByteArray());
		}
		catch (IOException e) {
			throw new MessageHandlingException(requestMessage,
					"failed to send HTTP request", e);
		}
	}

}
