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
import java.net.URL;
import java.nio.charset.Charset;

import org.springframework.integration.core.Message;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link OutboundRequestMapper}.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class DefaultOutboundRequestMapper implements OutboundRequestMapper {

	private final URL defaultUrl;

	private volatile String charset = "UTF-8";


	public DefaultOutboundRequestMapper(URL defaultUrl) {
		Assert.notNull(defaultUrl, "default URL must not be null");
		this.defaultUrl = defaultUrl;
	}


	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	public HttpRequest fromMessage(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "payload must not be null");
		String contentType = null;
		byte[] bytes = null;
		if (payload instanceof byte[]) {
			bytes = (byte[]) payload;
		}
		else if (payload instanceof String) {
			bytes = ((String) payload).getBytes(this.charset);
			contentType = "text/plain";
		}
		else if (payload instanceof Serializable) {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
			objectStream.writeObject(payload);
			objectStream.flush();
			objectStream.close();
			bytes = byteStream.toByteArray();
			contentType = "application/x-java-serialized-object";
		}
		else {
			throw new IllegalArgumentException(
					"payload must be a byte array, String, or Serializable object");
		}
		URL url = this.resolveUrl(message);
		String method = "POST"; // TODO: support GET for Map payload
		return new DefaultHttpRequest(url, method, bytes, contentType);
	}

	/**
	 * Resolve the request URL for the given Message. This implementation
	 * simply returns the default URL as provided to the constructor.  
	 */
	protected URL resolveUrl(Message<?> message) {
		return this.defaultUrl;
	}


	class DefaultHttpRequest implements HttpRequest {

		private final URL targetUrl;

		private final String requestMethod;

		private final ByteArrayOutputStream requestBody;

		private final String contentType;


		DefaultHttpRequest(URL targetUrl, String requestMethod, byte[] content, String contentType) throws IOException {
			Assert.notNull(targetUrl, "target url must not be null");
			this.targetUrl = targetUrl;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(content);
			this.requestBody = baos;
			this.contentType = contentType;
			this.requestMethod = (requestMethod != null) ? requestMethod : "POST";
		}

		public URL getTargetUrl() {
			return this.targetUrl;
		}

		public String getRequestMethod() {
			return this.requestMethod;
		}

		public ByteArrayOutputStream getBody() {
			return this.requestBody;
		}

		public Integer getContentLength() {
			return this.requestBody.size();
		}

		public String getContentType() {
			return this.contentType;
		}
	}

}
